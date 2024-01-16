package de.unimarburg.diz.kafkapseudonymizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import de.unimarburg.diz.kafkapseudonymizer.configuration.AppProperties;
import de.unimarburg.diz.kafkapseudonymizer.configuration.KafkaProperties;
import de.unimarburg.diz.kafkapseudonymizer.configuration.OutputTopicProperties;
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
    public void generateOutputTopicUsesMatchExpression(String input,
        String expected) {

        // arrange
        var props = new AppProperties(null, false, null, new KafkaProperties(
            new OutputTopicProperties("fhir-", "psn-fhir-")));

        var processor = new Processor(null, props);
        // act
        var result = processor.generateOutputTopic(input);
        // assert
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(value = {"fhir-patient:psn-fhir-patient",
        "fhir-lab:psn-fhir-lab"}, delimiter = ':')
    public void processSetsMessageHeaders(String input, String expected) {

        // arrange
        var props = new AppProperties(null, false, null, new KafkaProperties(
            new OutputTopicProperties("fhir-", "psn-fhir-")));

        var clientMock = Mockito.mock(PseudonymizerClient.class);
        Mockito
            .when(clientMock.process(any()))
            .thenReturn(new Bundle());

        var processor = new Processor(clientMock, props);
        var messageWithHeader = MessageBuilder
            .withPayload(new Bundle())
            .setHeaderIfAbsent(KafkaHeaders.RECEIVED_TOPIC, input);

        //act
        var result = processor
            .process()
            .apply(messageWithHeader.build());
        var inputTopic = result
            .getHeaders()
            .get("spring.cloud.stream.sendto.destination")
            .toString();

        //assert
        assertEquals(expected, inputTopic);
    }


    @Test
    public void computeOutputTopicFromInputTopicMatchExpressionException() {
        // arrange
        var props = new AppProperties(null, false, null, new KafkaProperties(
            new OutputTopicProperties("fhir\\-", "psn-fhir-")));

        // act
        assertThrows(IllegalArgumentException.class, () -> {
            String inputTopic = "test-fhir";
            var processor = new Processor(null, props);
            processor.generateOutputTopic(inputTopic);
        });
    }

    @Test
    public void generateOutputTopicFailsOnEmptyMatchExpression() {

        // arrange
        var props = new AppProperties(null, false, null,
            new KafkaProperties(new OutputTopicProperties("", "psn-fhir-")));

        // act
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> {
                var inputTopic = "test-fhir";
                var processor = new Processor(null, props);
                processor.generateOutputTopic(inputTopic);
            })
            .withMessage("Output topic match expression is empty");
    }

    @Test
    public void generateOutputTopicFailsOnEmptyReplacementExpression() {
        // arrange
        var props = new AppProperties(null, false, null,
            new KafkaProperties(new OutputTopicProperties("fhir-", "")));

        // act
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> {
                var inputTopic = "test-fhir";
                var processor = new Processor(null, props);
                processor.generateOutputTopic(inputTopic);
            })
            .withMessage("Output topic name replacement is empty");
    }


}
