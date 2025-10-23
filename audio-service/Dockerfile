FROM openjdk:21-jdk-slim

WORKDIR /app

# Копируем JAR файл
COPY target/*.jar app.jar

# Создаем папку логов с правами для всех
RUN mkdir -p /app/logs && chmod 777 /app/logs

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]