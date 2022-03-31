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
    private String generateTopicMatchExpression;
    private String generateTopicReplacement;

    @Autowired
    public Processor(PseudonymizerClient pseudonymizerClient,
        @Value("${services.kafka.generate-output-topic.match-expression}") String generateTopicMatchExpression,
        @Value("${services.kafka.generate-output-topic.replace-with}") String generateTopicReplacement) {
        this.pseudonymizerClient = pseudonymizerClient;
        this.generateTopicMatchExpression = generateTopicMatchExpression;
        this.generateTopicReplacement = generateTopicReplacement;

        log.info("Using match expression: {} with replacement: {}", generateTopicMatchExpression,
            generateTopicReplacement);
    }

    @Bean
    public Function<Message<Bundle>, Message<Bundle>> process() {
        return message -> {
            var bundle = message.getPayload();
            //pseudonomized the bundle
            var processed_msg = pseudonymizerClient.process(bundle);
            // Get message header key
            var messageKey = message
                .getHeaders()
                .getOrDefault(KafkaHeaders.RECEIVED_MESSAGE_KEY, "")
                .toString();
            // build new message and its key
            var messageBuilder = MessageBuilder
                .withPayload(processed_msg)
                .setHeaderIfAbsent(KafkaHeaders.MESSAGE_KEY, messageKey);
            // Get incoming topic
            var inputTopic = message
                .getHeaders()
                .get(KafkaHeaders.RECEIVED_TOPIC)
                .toString();
            log.debug("Incoming TOPIC: " + inputTopic);
            // send the bundle to the respective to input topic
            var outputTopic = computeOutputTopicFromInputTopic(inputTopic);
            messageBuilder.setHeader("spring.cloud.stream.sendto.destination", outputTopic);
            return messageBuilder.build();
        };
    }


    public String computeOutputTopicFromInputTopic(String inputTopic) {

        if (StringUtils.isNotBlank(generateTopicMatchExpression) && StringUtils.isNotBlank(
            generateTopicReplacement)) {
            String matches = inputTopic.replaceFirst(generateTopicMatchExpression,
                generateTopicReplacement);

            if (inputTopic.equals(matches)) {
                throw new IllegalArgumentException(String.format(
                    "Expression given at 'services.kafka.generate-output-topic.match-expression' not matched: %s",
                    generateTopicMatchExpression));
            }

            return matches;
        }
        throw new IllegalArgumentException(
            "Empty 'services.kafka.generate-output-topic.match-expression' or 'services.kafka.generate-output-topic.replace-with'");

    }
}

