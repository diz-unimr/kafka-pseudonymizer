package de.unimarburg.diz.kafkapseudonymizer;

import static net.logstash.logback.argument.StructuredArguments.kv;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import java.util.HashMap;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

public class PseudonymizerClient {

    private static final Logger log = LoggerFactory.getLogger(PseudonymizerClient.class);

    private final String pseudonymizerUrl;
    private final RetryTemplate retryTemplate;
    private final IGenericClient client;

    public PseudonymizerClient(
        FhirContext fhirContext, String pseudonymizerUrl, RetryTemplate retryTemplate) {
        this.client = fhirContext.newRestfulGenericClient(pseudonymizerUrl);
        this.pseudonymizerUrl = pseudonymizerUrl;
        this.retryTemplate = retryTemplate;
    }

    public PseudonymizerClient(FhirContext fhirContext, String pseudonymizerUrl) {
        this(fhirContext, pseudonymizerUrl, defaultTemplate());
    }

    public Bundle process(Bundle bundle) {
        log.debug(
            "Invoking pseudonymization service @ {}", kv("pseudonymizerUrl", pseudonymizerUrl));

        Parameters param = new Parameters();
        param.addParameter().setName("resource").setResource(bundle);

        return retryTemplate.execute(
            ctx ->
                client
                    .operation()
                    .onServer()
                    .named("de-identify")
                    .withParameters(param)
                    .returnResourceType(Bundle.class)
                    .execute());
    }

    public static RetryTemplate defaultTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(5000);
        backOffPolicy.setMultiplier(1.25);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        HashMap<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(HttpClientErrorException.class, false);
        retryableExceptions.put(HttpServerErrorException.class, true);
        retryableExceptions.put(ResourceAccessException.class, true);
        retryableExceptions.put(FhirClientConnectionException.class, true);

        RetryPolicy retryPolicy = new SimpleRetryPolicy(3, retryableExceptions);

        retryTemplate.setRetryPolicy(retryPolicy);

        retryTemplate.registerListener(
            new RetryListenerSupport() {
                @Override
                public <T, E extends Throwable> void onError(
                    RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
                    log.warn(
                        "HTTP Error occurred: {}. Retrying {}",
                        throwable.getMessage(), kv("attempt", context.getRetryCount()));
                }
            });

        return retryTemplate;
    }
}
