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
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.codesystems.V3ObservationValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PseudonymizerClientTests {

    @Test
    void processCallsPseudonymizationEndpoint() {
        // arrange
        var dummyUrl = "dummyUrl";
        var inputBundle = new Bundle();
        var expected = new Bundle();

        var fhirContext = mock(FhirContext.class);
        var fhirClient = setupClientMock(fhirContext, dummyUrl);
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

    private IGenericClient setupClientMock(FhirContext ctx, String url) {
        var fhirClient = mock(IGenericClient.class, RETURNS_DEEP_STUBS);
        when(ctx.newRestfulGenericClient(url)).thenReturn(fhirClient);
        return fhirClient;
    }

    @ParameterizedTest()
    @ValueSource(booleans = {true, false})
    void replaceSecurityTagsUsesAnonyedCoding(boolean replaceSecurityTags) {
        // arrange
        var dummyUrl = "dummyUrl";
        var pseuded = new Coding(V3ObservationValue.PSEUDED.getSystem(),
            V3ObservationValue.PSEUDED.toCode(),
            V3ObservationValue.PSEUDED.getDisplay());
        var anonyed = new Coding(V3ObservationValue.ANONYED.getSystem(),
            V3ObservationValue.ANONYED.toCode(),
            V3ObservationValue.ANONYED.getDisplay());

        var pseudedBundle = new Bundle();
        pseudedBundle
            .getMeta()
            .addSecurity(pseuded);
        var resource = new Patient();
        resource
            .getMeta()
            .addSecurity(pseuded);
        pseudedBundle
            .addEntry()
            .setResource(resource);

        // mocks & stubs
        var fhirContext = mock(FhirContext.class);
        var fhirClient = setupClientMock(fhirContext, dummyUrl);
        when(fhirClient
            .operation()
            .onServer()
            .named(eq("de-identify"))
            .withParameters(any())
            .returnResourceType(any())
            .execute()).thenReturn(pseudedBundle);

        // act
        var actual = new PseudonymizerClient(fhirContext,
            new PseudonymizerProperties(dummyUrl, replaceSecurityTags)).process(
            new Bundle());

        // assert
        assertThat(actual
            .getMeta()
            .getSecurityFirstRep())
            .usingRecursiveComparison()
            .isEqualTo(replaceSecurityTags ? anonyed : pseuded);
        assertThat(actual
            .getEntryFirstRep()
            .getResource()
            .getMeta()
            .getSecurityFirstRep())
            .usingRecursiveComparison()
            .isEqualTo(replaceSecurityTags ? anonyed : pseuded);
    }
}
