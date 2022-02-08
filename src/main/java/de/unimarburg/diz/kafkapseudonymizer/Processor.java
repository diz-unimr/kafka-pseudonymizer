package de.unimarburg.diz.kafkapseudonymizer;

import java.util.function.Function;

import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

@Service
public class Processor {

    private final PseudonymizerClient pseudonymizerClient;

    @Autowired
    public Processor(PseudonymizerClient fhirPseudonymizer) {
        this.pseudonymizerClient = fhirPseudonymizer;
    }
    @Bean
    public Function<KTable<String, Bundle>, KStream<String, Bundle>> process() {
        return fhirBundle -> fhirBundle.toStream().filter((k, v) -> v != null).mapValues(
            pseudonymizerClient::process);
    }

}
