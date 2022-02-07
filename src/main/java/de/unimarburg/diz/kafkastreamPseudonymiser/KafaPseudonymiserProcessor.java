package de.unimarburg.diz.kafkastreamPseudonymiser;

import java.util.function.Function;
import de.unimarburg.diz.FhirPseudonymizer;

import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.hl7.fhir.r4.model.Bundle;

public class KafaPseudonymiserProcessor {

    private final FhirPseudonymizer fhirPseudonymizer;

    @Autowired
    public KafaPseudonymiserProcessor(FhirPseudonymizer fhirPseudonymizer) {
        this.fhirPseudonymizer = fhirPseudonymizer;
    }
    @Bean
    public Function<KTable<String, Bundle>, KStream<String, Bundle>> process() {
        return fhir_bundle -> fhir_bundle.toStream().filter((k, v) -> v != null).mapValues(fhirPseudonymizer::process);
    }

}
