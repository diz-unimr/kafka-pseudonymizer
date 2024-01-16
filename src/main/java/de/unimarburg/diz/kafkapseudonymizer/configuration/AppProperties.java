package de.unimarburg.diz.kafkapseudonymizer.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "services")
@Validated
public record AppProperties(String inputTopic, boolean inputIsPattern,
                            PseudonymizerProperties pseudonymizer,
                            KafkaProperties kafka) {

}
