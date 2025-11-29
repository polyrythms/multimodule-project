FROM amazoncorretto:21-alpine

WORKDIR /app

# Копируем JAR файл
COPY target/*.jar app.jar

# Создаем папку логов
RUN mkdir -p /app/logs && chmod 777 /app/logs

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]