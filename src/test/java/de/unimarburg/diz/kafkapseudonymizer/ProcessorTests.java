package de.unimarburg.diz.kafkapseudonymizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static org.mockito.ArgumentMatchers.any;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

public class ProcessorTests {

//    @Mock
//    private PseudonymizerClient pseudonymizerClient;
//
//    @InjectMocks
//    private Processor processor;

    // 1. Use @ParameterizedTest
    @Test
    public void computeOutputTopicFromInputTopic_UsesMatchExpression() {
        // arrange
        var processor = new Processor(null,"fhir\\-", "psn-fhir-");
        var testInput = "fhir-lab";

        // act
        var result = processor.computeOutputTopicFromInputTopic(testInput);

        // assert
        assertThat(result.orElseThrow()).isEqualTo("psn-fhir-lab");
    }

    @Test
    @Disabled
    public void process_SetsMessageHeaders() {
        // arrange
        var inputBundle = new Bundle();
        var clientMock = Mockito.mock(PseudonymizerClient.class);
        Mockito.when(clientMock.process(any())).thenReturn(new Bundle());
        var inputMessage = new GenericMessage<>(inputBundle);

        // act
        var processor = new Processor(clientMock,"fhir\\-", "psn-fhir-");
        var result = processor.process().apply(inputMessage);
        // TODO
    }

}
