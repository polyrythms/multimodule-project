FROM amazoncorretto:21-alpine

WORKDIR /app

# Копируем JAR файл
COPY target/*.jar app.jar

# Копируем ресурсы (добавь эту строку)
COPY src/main/resources/ /app/resources/

# Создаем папку логов с правами для всех
RUN mkdir -p /app/logs && chmod 777 /app/logs

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]