// CHECKSTYLE:OFF
package de.unimarburg.diz.kafkapseudonymizer;

import static org.assertj.core.api.Fail.fail;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class TestContainerBase {

    protected static Network network = Network.newNetwork();
    @Container
    protected static KafkaContainer kafka = createKafkaContainer(network);
    @Container
    protected static GenericContainer fhirPseudonymizer = createPseudonymizerContainer(
        network);
    @Container
    protected static GenericContainer gPasContainer = createGpasContainer(
        network);

    protected static void setup() {
        initGpas(String.format("http://%s:%d", gPasContainer.getHost(),
            gPasContainer.getFirstMappedPort()));
    }

    private static GenericContainer createPseudonymizerContainer(
        Network network) {
        return new GenericContainer<>(
            DockerImageName.parse("ghcr.io/miracum/fhir-pseudonymizer:v2.12.0"))
            .withEnv(Collections.singletonMap("GPAS__URL",
                "http://gpas:8080/ttp-fhir/fhir/"))
            .withClasspathResourceMapping("anonymization.yaml",
                "/etc/anonymization.yaml", BindMode.READ_ONLY)
            .withNetwork(network)
            .waitingFor(Wait.forHttp("/fhir/metadata"))
            .withExposedPorts(8080);
    }


    private static GenericContainer createGpasContainer(Network network) {
        return new GenericContainer<>(DockerImageName.parse(
            "harbor.miracum.org/gpas/gpas:1.10.0-20201221"))
            .withNetworkAliases("gpas")
            .withNetwork(network)
            .withExposedPorts(8080)
            .waitingFor(Wait
                .forHealthcheck()
                .withStartupTimeout(Duration.ofMinutes(5)));
    }

    private static void initGpas(String host) {
        // patient domain
        var response = createDomain(host, "gpas/createPatientDomain.xml");
        if (response.statusCode() != 200) {
            fail("Error setting up gpas patient domain: " + response);
        }
    }

    private static HttpResponse<String> createDomain(String host, String file) {
        try {
            var request = HttpRequest
                .newBuilder()
                .uri(new URI(host + "/gpas/DomainService"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofFile(Path.of(ClassLoader
                    .getSystemResource(file)
                    .toURI())))
                .build();

            return HttpClient
                .newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            fail("Error create GPas Domain.", e.getMessage());
            return null;
        }
    }

    public static KafkaContainer createKafkaContainer(Network network) {
        return new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:5.5.0")).withNetwork(
            network);

    }

}
