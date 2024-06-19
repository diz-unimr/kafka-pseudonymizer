<img align="left" src="assets/kafka-pseudonymizer_96.png" alt="Kafka Pseudonymizer icon" width="96" height="96" >

# Kafka Pseudonymizer
[![MegaLinter](https://github.com/diz-unimr/kafka-pseudonymizer/actions/workflows/mega-linter.yml/badge.svg?branch=main)](https://github.com/diz-unimr/kafka-pseudonymizer/actions/workflows/mega-linter.yml?query=branch%3Amain) ![java](https://github.com/diz-unimr/kafka-pseudonymizer/actions/workflows/build.yml/badge.svg) ![docker](https://github.com/diz-unimr/kafka-pseudonymizer/actions/workflows/release.yml/badge.svg) [![codecov](https://codecov.io/gh/diz-unimr/kafka-pseudonymizer/graph/badge.svg?token=uaRbgoqlta)](https://codecov.io/gh/diz-unimr/kafka-pseudonymizer)

Kafka processor using the [FHIR® Pseudonymizer](https://github.com/miracum/fhir-pseudonymizer)
service to handle pseudonymization / anonymization of FHIR resources from
Kafka topics and send them to matching output topics.

It features a convention based approach when handling input and output
topics and supports using Regular Expressions to configure input topics.<br />
Providing a comma separated list of input topic names is supported,
too.<br />
Configurable match and replace values are used to determine output topic
names of consumed messages.

⚠ The current implementation supports pseudonymization/anonymization of Kafka
messages in FHIR 🔥 JSON only.

## <a name="deploy_config"></a> Configuration

The following environment variables can be set:

| Variable                                     | Default                                     | Description                                                                                                                 |
|----------------------------------------------|---------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| BOOTSTRAP_SERVERS                            | localhost:9092                              | Kafka brokers                                                                                                               |
| SECURITY_PROTOCOL                            | PLAINTEXT                                   | Kafka communication protocol                                                                                                |
| SSL_TRUST_STORE_LOCATION_INTERNAL            | /opt/kafka-pseudonymizer/ssl/truststore.jks | Truststore location                                                                                                         |
| SSL_TRUST_STORE_PASSWORD                     |                                             | Truststore password (if using `SECURITY_PROTOCOL=SSL`)                                                                      |
| SSL_KEY_STORE_LOCATION_INTERNAL              | /opt/kafka-pseudonymizer/ssl/keystore.jks   | Keystore location                                                                                                           |
| SSL_KEY_STORE_PASSWORD                       |                                             | Keystore password (if using `SECURITY_PROTOCOL=SSL`)                                                                        |
| SSL_TRUST_STORE_PASSWORD                     |                                             | Truststore password (if using `SECURITY_PROTOCOL=SSL`)                                                                      |
| INPUT_TOPICS                                 | `^(?=.*-fhir)(?!.*fhir-psn).*`              | Kafka input topic(s):  List of comma separated names or Regular expression (⚠ See also `INPUT_IS_PATTERN`️)                 |
| CONSUMER_THREADS                             | 4                                           | Number of concurrent Kafka consumer threads                                                                                 |
| INPUT_IS_PATTERN                             | true                                        | Sets type of `INPUT_TOPIC`: _true_ if it's a regexp, otherwise _false_                                                      |
| SERVICES_KAFKA_OUTPUT_TOPIC_MATCH_EXPRESSION | fhir-idat                                   | Part of the input message's topic name to replace with when determining the output topic                                    |
| SERVICES_KAFKA_OUTPUT_TOPIC_REPLACE_WITH     | fhir-psn                                    | Replaces this with the value of `SERVICES_KAFKA_OUTPUT_TOPIC_MATCH_EXPRESSION` to determine the message's output topic name |
| SERVICES_PSEUDONYMIZER_URL                   | `http://localhost:5000/fhir`                | Url of the [FHIR® Pseudonymizer](https://github.com/miracum/fhir-pseudonymizer) service                                     |
| SERVICES_PSEUDONYMIZER_DOMAIN_PREFIX         |                                             | Prefix for the gPAS target domains configured via the FHIR Pseudonymizer's `anonymization.yaml`                             |
| SSL_KEY_STORE_PASSWORD                       |                                             | Password of the Java keystore for Kafka authentication using SSL                                                            |
| SSL_TRUST_STORE_PASSWORD                     |                                             | Password of the Java truststore for Kafka authentication using SSL                                                          |

Additional application properties can be set by overriding values form the [application.yml](src/main/resources/application.yml) by using environment variables.

## Deployment

Example via `docker compose`:
```yml
kafka-pseudonymizer:
  image: ghcr.io/diz-unimr/kafka-pseudonymizer:latest
  restart: unless-stopped
  environment:
    BOOTSTRAP_SERVERS: localhost:9092
    SECURITY_PROTOCOL: SSL
    INPUT_TOPICS: "^(?=.*-fhir)(?!.*fhir-psn).*"
    INPUT_IS_PATTERN: "true"
    CONSUMER_THREADS: 6
    SERVICES_KAFKA_OUTPUT_TOPIC_MATCH_EXPRESSION: fhir-idat
    SERVICES_KAFKA_OUTPUT_TOPIC_REPLACE_WITH: fhir-psn
    SERVICES_PSEUDONYMIZER_URL: http://localhost:5000/fhir
    SERVICES_PSEUDONYMIZER_DOMAIN_PREFIX: miracum-
    SSL_KEY_STORE_PASSWORD: private-key-password
    SSL_TRUST_STORE_PASSWORD: store-password
  volumes:
    - ./cert/keystore.jks:/opt/kafka-pseudonymizer/ssl/keystore.jks:ro
    - ./cert/truststore.jks:/opt/kafka-pseudonymizer/ssl/truststore.jks:ro
```

## License

[AGPL-3.0](https://www.gnu.org/licenses/agpl-3.0.en.html)
