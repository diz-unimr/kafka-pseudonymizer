package de.unimarburg.diz.kafkapseudonymizer.configuration;

import ca.uhn.fhir.context.FhirContext;
import de.unimarburg.diz.kafkapseudonymizer.PseudonymizerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationPropertiesScan
public class FhirConfiguration {


    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    @Autowired
    public PseudonymizerClient pseudonymizer(FhirContext fhirContext,
        AppProperties properties) {

        return new PseudonymizerClient(fhirContext, properties.pseudonymizer());
    }

}
