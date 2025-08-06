FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY target/telegram-bot-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]