package de.unimarburg.diz.kafkastreamPseudonymiser;

import java.util.function.Function;
import de.unimarburg.diz.FhirPseudonymizer;

import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

public class KafaPseudonymiserProcessor {

    @Autowired
    private final FhirPseudonymizer fhirPseudonymizer;

    public KafaPseudonymiserProcessor(FhirPseudonymizer fhirPseudonymizer) {
        this.fhirPseudonymizer = fhirPseudonymizer;
    }


    @Bean
    public Function<KTable<String, String>, KStream<String, String>> process() {
        return input -> input.toStream().filter((k, v) -> v != null);
    }

}
