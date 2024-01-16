package de.unimarburg.diz.kafkapseudonymizer.configuration;

public record OutputTopicProperties(String matchExpression,
                                    String replaceWith) {

}
