package de.unimarburg.diz.kafkapseudonymizer;

import static net.logstash.logback.argument.StructuredArguments.kv;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import com.google.common.base.Strings;
import de.unimarburg.diz.kafkapseudonymizer.configuration.PseudonymizerProperties;
import java.util.HashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.codesystems.V3ObservationValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

public class PseudonymizerClient {

    private static final Logger LOG =
        LoggerFactory.getLogger(PseudonymizerClient.class);
    private static final long BACKOFF_INITIAL_INTERVAL = 5000;
    private static final double BACKOFF_MULTIPLIER = 1.25;
    private static final int RETRY_MAX_ATTEMPTS = 3;
    private final PseudonymizerProperties properties;
    private final RetryTemplate retryTemplate;
    private final IGenericClient client;

    private final Predicate<Coding> isPseuded =
        s -> s.getCode().equals(V3ObservationValue.PSEUDED.toCode());

    public PseudonymizerClient(FhirContext fhirContext,
        PseudonymizerProperties properties, RetryTemplate retryTemplate) {
        this.properties = properties;
        this.client = fhirContext.newRestfulGenericClient(properties.url());
        this.retryTemplate = retryTemplate;
    }

    public PseudonymizerClient(FhirContext fhirContext,
        PseudonymizerProperties properties) {
        this(fhirContext, properties, defaultTemplate());
    }

    public Bundle process(Bundle bundle) {
        LOG.debug("Invoking pseudonymization service @ {}",
            kv("pseudonymizerUrl", properties.url()));

        Parameters param = new Parameters();
        param.addParameter().setName("resource").setResource(bundle);
        if (!Strings.isNullOrEmpty(properties.domainPrefix())) {
            param.addParameter().setName("settings").addPart(
                new ParametersParameterComponent().setName("domain-prefix")
                    .setValue(new StringType(properties.domainPrefix())));
        }

        var result = retryTemplate.execute(
            ctx -> client.operation().onServer().named("de-identify")
                .withParameters(param).returnResourceType(Bundle.class)
                .execute());

        return replaceSecurityTags(result);
    }

    Bundle replaceSecurityTags(Bundle bundle) {
        if (properties.replaceSecurityTag()) {
            var pseudedCodings = Stream.concat(
                bundle.getMeta().getSecurity().stream().filter(isPseuded),

                bundle.getEntry().stream()
                    .filter(BundleEntryComponent::hasResource).flatMap(
                        x -> x.getResource().getMeta().getSecurity().stream())
                    .filter(isPseuded)).toList();

            pseudedCodings.forEach(c -> {
                c.setSystem(V3ObservationValue.ANONYED.getSystem());
                c.setCode(V3ObservationValue.ANONYED.toCode());
                c.setDisplay(V3ObservationValue.ANONYED.getDisplay());
            });
        }

        return bundle;
    }

    public static RetryTemplate defaultTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(BACKOFF_INITIAL_INTERVAL);
        backOffPolicy.setMultiplier(BACKOFF_MULTIPLIER);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        var retryable = new HashMap<Class<? extends Throwable>, Boolean>();
        retryable.put(HttpClientErrorException.class, false);
        retryable.put(HttpServerErrorException.class, true);
        retryable.put(ResourceAccessException.class, true);
        retryable.put(FhirClientConnectionException.class, true);

        var retryPolicy = new SimpleRetryPolicy(RETRY_MAX_ATTEMPTS, retryable);

        retryTemplate.setRetryPolicy(retryPolicy);

        retryTemplate.registerListener(new RetryListener() {
            @Override
            public <T, E extends Throwable> void onError(RetryContext context,
                RetryCallback<T, E> callback, Throwable throwable) {
                LOG.warn("HTTP Error occurred: {}. Retrying {}",
                    throwable.getMessage(),
                    kv("attempt", context.getRetryCount()));
            }
        });

        return retryTemplate;
    }
}
