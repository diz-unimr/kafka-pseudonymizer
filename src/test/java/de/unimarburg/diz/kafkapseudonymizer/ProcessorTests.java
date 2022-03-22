package de.unimarburg.diz.kafkapseudonymizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

public class ProcessorTests {

//    @Mock
//    private PseudonymizerClient pseudonymizerClient;
//
//    @InjectMocks
//    private Processor processor;

    // 1. Use @ParameterizedTest

    @ParameterizedTest
    @ValueSource(strings = {"fhir-lab","lab-fhir","fhirr-lab", "fhrri-lab",""} )
    public void computeOutputTopicFromInputTopic_UsesMatchExpression(String testInput) {
        // arrange
        var processor = new Processor(null,"fhir\\-", "psn-fhir-");

        // act
        var result = processor.computeOutputTopicFromInputTopic(testInput);

        // assert
        assertThat(result.orElseThrow()).isEqualTo("psn-fhir-lab");
    }



    @ParameterizedTest
    @ValueSource(strings = {"fhir-patient","fhir-lab","","hjff"} )
    public void process_SetsMessageHeaders(String input) {
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
        assertEquals("psn-fhir-patient",inputTopic);

    }

}
