package de.unimarburg.diz.kafkapseudonymizer;


import de.unimarburg.diz.kafkapseudonymizer.configuration.AppProperties;
import java.util.Objects;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
public class Processor {

    private static final Logger LOG = LoggerFactory.getLogger(Processor.class);
    private final PseudonymizerClient pseudonymizerClient;
    private final AppProperties props;

    @Autowired
    public Processor(PseudonymizerClient pseudonymizerClient,
        AppProperties props) {
        if (StringUtils.isBlank(
            props.kafka().outputTopic().matchExpression())) {
            throw new IllegalArgumentException(
                "Output topic match expression is empty");
        }
        if (StringUtils.isBlank(props.kafka().outputTopic().replaceWith())) {
            throw new IllegalArgumentException(
                "Output topic name replacement is empty");
        }

        this.pseudonymizerClient = pseudonymizerClient;
        this.props = props;

        LOG.info("Using match expression: {} with replacement: {}",
            props.kafka().outputTopic().matchExpression(),
            props.kafka().outputTopic().replaceWith());
    }

    @Bean
    public Function<Message<Bundle>, Message<Bundle>> process() {
        return message -> {

            // get message header key
            var messageKey =
                message.getHeaders().getOrDefault(KafkaHeaders.RECEIVED_KEY, "")
                    .toString();

            // incoming topic
            var inputTopic = Objects.requireNonNull(
                    message.getHeaders().get(KafkaHeaders.RECEIVED_TOPIC))
                .toString();
            LOG.info("Processing message {} from topic: {}", messageKey,
                inputTopic);

            // determine output topic
            var outputTopic = generateOutputTopic(inputTopic);

            // pseudonymize the bundle
            var bundle = message.getPayload();
            var processed = pseudonymizerClient.process(bundle);

            // build new message with payload
            var messageBuilder = MessageBuilder.withPayload(processed)
                .setHeader(KafkaHeaders.KEY, messageKey)
                .setHeader(KafkaHeaders.TIMESTAMP,
                    message.getHeaders().get(KafkaHeaders.RECEIVED_TIMESTAMP))
                .setHeader("spring.cloud.stream.sendto.destination",
                    outputTopic);

            return messageBuilder.build();
        };
    }


    public String generateOutputTopic(String inputTopic) {
        var outputTopic = inputTopic.replaceFirst(
            props.kafka().outputTopic().matchExpression(),
            props.kafka().outputTopic().replaceWith());

        if (inputTopic.equals(outputTopic)) {
            throw new IllegalArgumentException(
                String.format("Match expression' not matched: %s",
                    props.kafka().outputTopic().matchExpression()));
        }

        // validate output topic is not matched by
        // the input topic's expression (would cause a loop)
        if (props.inputIsPattern() && outputTopic.matches(
            props.kafka().outputTopic().matchExpression())) {
            throw new IllegalArgumentException(String.format(
                "Input topic pattern '%s' matches output topic pattern '%s'."
                    + " This would cause a loop.", props.inputTopic(),
                outputTopic));
        }

        return outputTopic;
    }
}

