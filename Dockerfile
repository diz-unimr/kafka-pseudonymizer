FROM eclipse-temurin:17.0.11_9-jdk-jammy AS build
WORKDIR /home/gradle/src
ENV GRADLE_USER_HOME /gradle

COPY . .
RUN ./gradlew build --info && \
    java -Djarmode=layertools -jar build/libs/*.jar extract

FROM gcr.io/distroless/java17:nonroot
WORKDIR /opt/kafka-pseudonymizer
COPY --from=build /home/gradle/src/dependencies/ ./
COPY --from=build /home/gradle/src/spring-boot-loader/ ./
COPY --from=build /home/gradle/src/application/ ./
COPY HealthCheck.java .

USER nonroot
ARG GIT_REF=""
ARG GIT_URL=""
ARG BUILD_TIME=""
ARG VERSION=0.0.0
ENV APP_VERSION=${VERSION} \
    SPRING_PROFILES_ACTIVE="prod"
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=90", "org.springframework.boot.loader.launch.JarLauncher"]

HEALTHCHECK --interval=25s --timeout=3s --retries=2 CMD ["java", "HealthCheck.java", "||", "exit", "1"]

LABEL org.opencontainers.image.created=${BUILD_TIME} \
    org.opencontainers.image.authors="" \
    org.opencontainers.image.source=${GIT_URL} \
    org.opencontainers.image.version=${VERSION} \
    org.opencontainers.image.revision=${GIT_REF} \
    org.opencontainers.image.vendor="diz.uni-marburg.de" \
    org.opencontainers.image.title="kafka-pseudonymizer" \
    org.opencontainers.image.description="Kafka pseudonymizer"
