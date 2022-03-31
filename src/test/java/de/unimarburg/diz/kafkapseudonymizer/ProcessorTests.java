package de.unimarburg.diz.kafkapseudonymizer;

import static org.assertj.core.api.Assertions.assertThat;
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

    private final String emptyExpressionExceptionMessage = "Empty 'services.kafka.generate-output-topic.match-expression' or 'services.kafka.generate-output-topic.replace-with'";

    private static Stream<Arguments> provideStringForInputAndOutputTopics() {
        return Stream.of(Arguments.of("fhir-lab", "psn-fhir-lab"),
            Arguments.of("fhir-lufu", "psn-fhir-lufu"));
    }

    @ParameterizedTest
    //@CsvSource(value = {"fhir-lab:psn-fhir-lab","fhir-lufu:psn-fhir-lufu"}, delimiter = ':')
    @MethodSource("provideStringForInputAndOutputTopics")
    public void computeOutputTopicFromInputTopic_UsesMatchExpression(String input,
        String extected) {
        // arrange
        var processor = new Processor(null, "fhir\\-", "psn-fhir-");
        // act
        var result = processor.computeOutputTopicFromInputTopic(input);
        // assert
        assertThat(result).isEqualTo(extected);
    }

    @ParameterizedTest
    @CsvSource(value = {"fhir-patient:psn-fhir-patient", "fhir-lab:psn-fhir-lab"}, delimiter = ':')
    public void process_SetsMessageHeaders(String input, String extected) {
        // arrange
        var clientMock = Mockito.mock(PseudonymizerClient.class);
        Mockito
            .when(clientMock.process(any()))
            .thenReturn(new Bundle());
        var processor = new Processor(clientMock, "fhir\\-", "psn-fhir-");
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
            var processor = new Processor(null, "fhir\\-", "psn-fhir-");
            processor.computeOutputTopicFromInputTopic(inputTopic);
        });
    }

    @Test
    public void computeOutputTopicFromInputTopic_Empty_MatchExpressionException() {

        // act
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
            String inputTopic = "test-fhir";
            var processor = new Processor(null, "", "psn-fhir-");
            processor.computeOutputTopicFromInputTopic(inputTopic);
        });
        assertEquals(emptyExpressionExceptionMessage, exception.getMessage());
    }

    @Test
    public void computeOutputTopicFromInputTopic_Empty_ReplacementExpressionException() {
        // act
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
            String inputTopic = "test-fhir";
            var processor = new Processor(null, "fhir\\-", "");
            processor.computeOutputTopicFromInputTopic(inputTopic);
        });
        assertEquals(emptyExpressionExceptionMessage, exception.getMessage());
    }


}
