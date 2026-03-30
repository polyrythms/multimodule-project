# Stage 1: Builder
FROM maven:3.9-amazoncorretto-21 AS builder

WORKDIR /build

# Копируем только pom.xml для кэширования зависимостей
COPY pom.xml .
COPY kafka-commons/pom.xml ./kafka-commons/pom.xml
COPY audio-service/pom.xml ./audio-service/pom.xml
# COPY telegram-bot/pom.xml ./telegram-bot/pom.xml  # ЗАКОММЕНТИРОВАНО - не нужно для audio-service

# Скачиваем зависимости
RUN mvn dependency:go-offline -B

# Копируем исходники
COPY kafka-commons/src ./kafka-commons/src
COPY audio-service/src ./audio-service/src
# COPY telegram-bot/src ./telegram-bot/src  # ЗАКОММЕНТИРОВАНО - не нужно для audio-service

# Сборка
RUN mvn clean package -pl audio-service -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Безопасность: создаем пользователя и устанавливаем su-exec
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    apk add --no-cache curl su-exec

WORKDIR /app

# Копируем JAR из builder
COPY --from=builder /build/audio-service/target/*.jar app.jar

# Копируем общий entrypoint.sh
COPY scripts/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Создаем папку для логов
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app/logs

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# Используем общий entrypoint
ENTRYPOINT ["/entrypoint.sh"]