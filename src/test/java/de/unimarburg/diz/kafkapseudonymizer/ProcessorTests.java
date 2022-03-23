package de.unimarburg.diz.kafkapseudonymizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.GenericMessage;

import java.util.stream.Stream;

public class ProcessorTests {
    @ParameterizedTest
    //@CsvSource(value = {"fhir-lab:psn-fhir-lab","fhir-lufu:psn-fhir-lufu"}, delimiter = ':')
    @MethodSource("provideStringForInputAndOutputTopics")
    public void computeOutputTopicFromInputTopic_UsesMatchExpression(String input, String extected) {
        // arrange
        var processor = new Processor(null,"fhir\\-", "psn-fhir-");
        // act
        var result = processor.computeOutputTopicFromInputTopic(input);
        // assert
        assertThat(result.orElseThrow()).isEqualTo(extected);
    }



    private static Stream<Arguments> provideStringForInputAndOutputTopics(){
        return Stream.of(
            Arguments.of("fhir-lab","psn-fhir-lab"),
            Arguments.of("lufu-fhir","lufu-fhir"),
            Arguments.of("fhri-lab","fhri-lab")
        );
    }



    @ParameterizedTest
    @CsvSource(value = {"fhir-patient:psn-fhir-patient","lab-fhir:lab-fhir"},delimiter = ':')
    public void process_SetsMessageHeaders(String input,String extected) {
        // arrange
        var inputBundle = new Bundle();
        var inputMessage = new GenericMessage<>(inputBundle);
        var clientMock = Mockito.mock(PseudonymizerClient.class);
        Mockito.when(clientMock.process(any())).thenReturn(new Bundle());
        var processor = new Processor(clientMock,"fhir\\-", "psn-fhir-");
        var pseudo = clientMock.process(inputMessage.getPayload());
        var message_with_header = MessageBuilder.withPayload(pseudo)
            .setHeaderIfAbsent(KafkaHeaders.RECEIVED_TOPIC, input);
        //act
        var result = processor.process().apply(message_with_header.build());
        var inputTopic =result.getHeaders().get("spring.cloud.stream.sendto.destination").toString();
        //assert
        assertEquals(extected,inputTopic);
    }

}
