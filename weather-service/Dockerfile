# Stage 1: Builder
FROM maven:3.9-amazoncorretto-21 AS builder

WORKDIR /build

# Копируем pom-файлы
COPY pom.xml .
COPY kafka-commons/pom.xml ./kafka-commons/pom.xml
COPY telegram-bot/pom.xml ./telegram-bot/pom.xml
COPY audio-service/pom.xml ./audio-service/pom.xml
COPY weather-service/pom.xml ./weather-service/pom.xml

RUN mvn dependency:go-offline -B

# Копируем исходники только weather-service (и kafka-commons, если нужно, но weather-service не использует kafka-commons)
COPY kafka-commons/src ./kafka-commons/src
COPY weather-service/src ./weather-service/src

RUN mvn clean package -pl weather-service -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    apk add --no-cache curl su-exec

WORKDIR /app

COPY --from=builder /build/weather-service/target/*.jar app.jar
COPY scripts/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

RUN mkdir -p /app/logs && chown -R appuser:appgroup /app/logs

HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]