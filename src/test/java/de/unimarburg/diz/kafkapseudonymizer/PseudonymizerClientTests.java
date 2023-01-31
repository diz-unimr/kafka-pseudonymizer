package de.unimarburg.diz.kafkapseudonymizer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.unimarburg.diz.kafkapseudonymizer.configuration.PseudonymizerProperties;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;

public class PseudonymizerClientTests {

    @Test
    public void process_CallsPseudonymizationEndpoint() {
        // arrange
        var dummyUrl = "dummyUrl";
        var inputBundle = new Bundle();
        var expected = new Bundle();

        // mocks & stubs
        var fhirContext = mock(FhirContext.class);
        var fhirClient = mock(IGenericClient.class, RETURNS_DEEP_STUBS);
        when(fhirContext.newRestfulGenericClient(dummyUrl)).thenReturn(fhirClient);
        when(fhirClient
            .operation()
            .onServer()
            .named(eq("de-identify"))
            .withParameters(argThat((Parameters p) -> p
                .getParameter()
                .stream()
                .anyMatch(__ -> __
                    .getName()
                    .equals("resource") && __
                    .getResource()
                    .equals(inputBundle))))
            .returnResourceType(any())
            .execute()).thenReturn(expected);

        // act
        var actual = new PseudonymizerClient(fhirContext,
            new PseudonymizerProperties(dummyUrl, false)).process(inputBundle);

        // assert
        assertThat(actual).isEqualTo(expected);
    }
}
