package de.unimarburg.diz.kafkapseudonymizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;

public class ProcessorTests {

    private static Stream<Arguments> provideStringForInputAndOutputTopics() {
        return Stream.of(Arguments.of("fhir-lab", "psn-fhir-lab"),
            Arguments.of("fhir-lufu", "psn-fhir-lufu"));
    }

    @ParameterizedTest
    @MethodSource("provideStringForInputAndOutputTopics")
    public void generateOutputTopic_UsesMatchExpression(String input, String expected) {
        // arrange
        var processor = new Processor(null, "fhir-", "psn-fhir-", null, false);
        // act
        var result = processor.generateOutputTopic(input);
        // assert
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {"fhir-patient:psn-fhir-patient", "fhir-lab:psn-fhir-lab"}, delimiter = ':')
    public void process_SetsMessageHeaders(String input, String extected) {
        // arrange
        var clientMock = Mockito.mock(PseudonymizerClient.class);
        Mockito
            .when(clientMock.process(any()))
            .thenReturn(new Bundle());
        var processor = new Processor(clientMock, "fhir-", "psn-fhir-", null, false);
        var message_with_header = MessageBuilder
            .withPayload(new Bundle())
            .setHeaderIfAbsent(KafkaHeaders.RECEIVED_TOPIC, input);

        //act
        var result = processor
            .process()
            .apply(message_with_header.build());
        var inputTopic = result
            .getHeaders()
            .get("spring.cloud.stream.sendto.destination")
            .toString();

        //assert
        assertEquals(extected, inputTopic);
    }


    @Test
    public void computeOutputTopicFromInputTopic_MatchExpressionException() {

        // act
        assertThrows(IllegalArgumentException.class, () -> {
            String inputTopic = "test-fhir";
            var processor = new Processor(null, "fhir\\-", "psn-fhir-", null, false);
            processor.generateOutputTopic(inputTopic);
        });
    }

    @Test
    public void generateOutputTopic_Empty_MatchExpressionException() {

        // act
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> {
                var inputTopic = "test-fhir";
                var processor = new Processor(null, "", "psn-fhir-", null, false);
                processor.generateOutputTopic(inputTopic);
            })
            .withMessage(
                "Property 'services.kafka.generate-output-topic.match-expression' is empty");
    }

    @Test
    public void generateOutputTopic_Empty_ReplacementExpressionException() {
        // act
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> {
                var inputTopic = "test-fhir";
                var processor = new Processor(null, "fhir-", "", null, false);
                processor.generateOutputTopic(inputTopic);
            })
            .withMessage("Property 'services.kafka.generate-output-topic.replace-with' is empty");
    }


}
