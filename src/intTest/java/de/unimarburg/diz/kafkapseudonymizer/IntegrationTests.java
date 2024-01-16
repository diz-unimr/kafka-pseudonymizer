package de.unimarburg.diz.kafkapseudonymizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.miracum.kafka.serializers.KafkaFhirDeserializer;
import org.miracum.kafka.serializers.KafkaFhirSerializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "services.kafka.generate-output-topic.match-expression=fhir",
    "services.kafka.generate-output-topic.replace-with=fhir-psn",
    "spring.cloud.stream.bindings.process-in-0.destination=test-fhir(?!-psn).*",
    "spring.cloud.stream.kafka.bindings.process-in-0.consumer."
        + "destinationIsPattern=true"})
class IntegrationTests extends TestContainerBase {


    @DynamicPropertySource
    private static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrapServers",
            kafka::getBootstrapServers);
        registry.add("services.pseudonymizer.url",
            () -> "http://" + fhirPseudonymizer.getHost() + ":"
                + fhirPseudonymizer.getFirstMappedPort() + "/fhir");
    }

    @BeforeAll
    public static void setupContainers() {
        setup();

        // produce message to input topic
        // note: this creates the input topic which must exist before the
        // kafka binder resolves the input topic expression
        var producer = createProducer();

        // create input bundle
        var bundle = new Bundle().addEntry(
            new BundleEntryComponent().setResource(
                new Patient().setId("123456")));
        var record = new ProducerRecord<String, Bundle>("test-fhir", bundle);
        // .. and send
        producer.send(record);
    }

    private static KafkaProducer<String, Bundle> createProducer() {
        var producerProps = new HashMap<String, Object>();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            kafka.getBootstrapServers());
        producerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-producer");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            KafkaFhirSerializer.class);
        return new KafkaProducer<>(producerProps);
    }

    @Test
    public void processProducesPseudonymizedMessage() {
        // create consumer to read from the output topic
        var outputTopic = "test-fhir-psn";
        var consumer = createConsumer();
        consumer.subscribe(List.of(outputTopic));
        var consumed = KafkaTestUtils.getSingleRecord(consumer, outputTopic,
            Duration
                .ofMinutes(2)
                .toMillis());

        var pseudonymizedPatient = (Patient) consumed
            .value()
            .getEntryFirstRep()
            .getResource();

        var pseudedCoding = new Coding(
            "http://terminology.hl7.org/CodeSystem/v3-ObservationValue",
            "PSEUDED", "pseudonymized");

        assertThat(pseudonymizedPatient)
            .extracting(r -> r
                .getMeta()
                .getSecurityFirstRep())
            .usingRecursiveComparison()
            .isEqualTo(pseudedCoding);
    }

    private KafkaConsumer<String, Bundle> createConsumer() {
        var consumerProps = new HashMap<String, Object>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            KafkaFhirDeserializer.class);
        consumerProps.put(ConsumerConfig.CLIENT_ID_CONFIG, "test-consumer");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new KafkaConsumer<>(consumerProps);
    }

}
