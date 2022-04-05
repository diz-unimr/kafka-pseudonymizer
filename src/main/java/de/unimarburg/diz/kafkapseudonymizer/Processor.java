package de.unimarburg.diz.kafkapseudonymizer;


import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
public class Processor {

    private static final Logger log = LoggerFactory.getLogger(Processor.class);
    private final PseudonymizerClient pseudonymizerClient;
    private final String generateTopicMatchExpression;
    private final String generateTopicReplacement;
    private final String inputMatch;
    private final boolean inputIsPattern;

    @Autowired
    public Processor(PseudonymizerClient pseudonymizerClient,
        @Value("${services.kafka.generate-output-topic.match-expression}") String generateTopicMatchExpression,
        @Value("${services.kafka.generate-output-topic.replace-with}") String generateTopicReplacement,
        @Value("${spring.cloud.stream.bindings.process-in-0.destination}") String inputMatch,
        @Value("${spring.cloud.stream.kafka.bindings.process-in-0.consumer.destinationIsPattern}") Boolean inputIsPattern) {
        if (StringUtils.isBlank(generateTopicMatchExpression)) {
            throw new IllegalArgumentException(
                "Property 'services.kafka.generate-output-topic.match-expression' is empty");
        }
        if (StringUtils.isBlank(generateTopicReplacement)) {
            throw new IllegalArgumentException(
                "Property 'services.kafka.generate-output-topic.replace-with' is empty");
        }

        this.pseudonymizerClient = pseudonymizerClient;
        this.generateTopicMatchExpression = generateTopicMatchExpression;
        this.generateTopicReplacement = generateTopicReplacement;
        this.inputMatch = inputMatch;
        this.inputIsPattern = inputIsPattern;

        log.info("Using match expression: {} with replacement: {}", generateTopicMatchExpression,
            generateTopicReplacement);
    }

    @Bean
    public Function<Message<Bundle>, Message<Bundle>> process() {
        return message -> {
            // incoming topic
            var inputTopic = message
                .getHeaders()
                .get(KafkaHeaders.RECEIVED_TOPIC)
                .toString();
            log.debug("Incoming TOPIC: " + inputTopic);
            // determine output topic
            var outputTopic = generateOutputTopic(inputTopic);

            //pseudonymize the bundle
            var bundle = message.getPayload();
            var processed_msg = pseudonymizerClient.process(bundle);

            // get message header key
            var messageKey = message
                .getHeaders()
                .getOrDefault(KafkaHeaders.RECEIVED_MESSAGE_KEY, "")
                .toString();
            // build new message with payload
            var messageBuilder = MessageBuilder
                .withPayload(processed_msg)
                .setHeaderIfAbsent(KafkaHeaders.MESSAGE_KEY, messageKey);
            messageBuilder.setHeader("spring.cloud.stream.sendto.destination", outputTopic);

            return messageBuilder.build();
        };
    }


    public String generateOutputTopic(String inputTopic) {
        var outputTopic = inputTopic.replaceFirst(generateTopicMatchExpression,
            generateTopicReplacement);

        if (inputTopic.equals(outputTopic)) {
            throw new IllegalArgumentException(String.format(
                "Expression given at 'services.kafka.generate-output-topic.match-expression' not matched: %s",
                generateTopicMatchExpression));
        }

        // validate output topic is not matched by the input topic's expression (would cause a loop)
        if (inputIsPattern && outputTopic.matches(inputMatch)) {
            throw new IllegalArgumentException(String.format(
                "Input topic pattern '%s' matches output topic pattern '%s'. This would cause a loop.",
                inputMatch, outputTopic));
        }

        return outputTopic;
    }
}

