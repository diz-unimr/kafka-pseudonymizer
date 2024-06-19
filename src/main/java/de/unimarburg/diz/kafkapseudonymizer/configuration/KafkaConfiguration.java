package de.unimarburg.diz.kafkapseudonymizer.configuration;

import org.apache.kafka.common.serialization.Serde;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.miracum.kafka.serializers.KafkaFhirSerde;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler;

@Configuration
@EnableKafka
public class KafkaConfiguration {

    @Bean
    public Serde<IBaseResource> fhirSerde() {
        return new KafkaFhirSerde();
    }

    @SuppressWarnings("checkstyle:LineLength")
    @Bean
    ListenerContainerCustomizer<AbstractMessageListenerContainer<?, ?>> customizer() {
        return (container, destinationName, group) -> {
            var handler = new CommonContainerStoppingErrorHandler();
            container.setCommonErrorHandler(handler);
        };
    }
}
