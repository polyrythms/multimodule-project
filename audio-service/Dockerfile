# Stage 1: Builder
FROM maven:3.9-amazoncorretto-21 AS builder

WORKDIR /build

# Копируем ВСЕ pom.xml (нужны для корректной работы Maven Reactor)
COPY pom.xml .
COPY kafka-commons/pom.xml ./kafka-commons/pom.xml
COPY telegram-bot/pom.xml ./telegram-bot/pom.xml
COPY audio-service/pom.xml ./audio-service/pom.xml
COPY auth-service/pom.xml ./auth-service/pom.xml
COPY weather-service/pom.xml ./weather-service/pom.xml

# Скачиваем зависимости
RUN mvn dependency:go-offline -B

# Копируем ТОЛЬКО нужные исходники
COPY kafka-commons/src ./kafka-commons/src
COPY audio-service/src ./audio-service/src
# НЕ копируем telegram-bot/src (это сэкономит время и кэш)

# Сборка только audio-service
RUN mvn clean package -pl audio-service -am -DskipTests

# Stage 2: Runtime (без изменений)
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    apk add --no-cache curl su-exec

WORKDIR /app

COPY --from=builder /build/audio-service/target/*.jar app.jar
COPY scripts/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

RUN mkdir -p /app/logs && chown -R appuser:appgroup /app/logs

HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["/entrypoint.sh"]