FROM amazoncorretto:21-alpine

WORKDIR /app

# Копируем JAR файл
COPY target/*.jar app.jar

# Копируем ресурсы
COPY src/main/resources/ /app/resources/

# ⭐ ДОБАВЛЯЕМ: Копируем файлы миграций Liquibase
COPY db/ /app/resources/db/

# Создаем папку логов
RUN mkdir -p /app/logs && chmod 777 /app/logs

# Проверяем что файлы скопированы
RUN find /app/resources -name "*.yaml" | head -10

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]