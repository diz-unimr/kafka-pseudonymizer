package de.unimarburg.diz.kafkapseudonymizer.configuration;

public record PseudonymizerProperties(String url, String domainPrefix,
                                      boolean replaceSecurityTag) {

}
