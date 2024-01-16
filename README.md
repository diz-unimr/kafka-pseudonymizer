<img src="assets/kafka-pseudonymizer_96.png" alt="Kafka Pseudonymizer icon" width="96" height="96" style="float:left;margin:20px" />

# Kafka Pseudonymizer
[![codecov](https://codecov.io/gh/diz-unimr/kafka-pseudonymizer/graph/badge.svg?token=uaRbgoqlta)](https://codecov.io/gh/diz-unimr/kafka-pseudonymizer)

Kafka pseudonymization/anonymization processor featuring a convention based approach when handling input and output topics.

The current implementation supports pseudonymization/anonymization of Kafka messages in FHIR 🔥 format only.

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
    SSL_KEY_STORE_PASSWORD: private-key-password
    SSL_TRUST_STORE_PASSWORD: store-password
  volumes:
    - ./cert/keystore.jks:/opt/kafka-pseudonymizer/ssl/keystore.jks:ro
    - ./cert/truststore.jks:/opt/kafka-pseudonymizer/ssl/truststore.jks:ro
```

## License

[AGPL-3.0](https://www.gnu.org/licenses/agpl-3.0.en.html)
