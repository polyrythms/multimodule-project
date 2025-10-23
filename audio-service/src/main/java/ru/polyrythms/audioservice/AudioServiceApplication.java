package ru.polyrythms.audioservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;


@SpringBootApplication
@Slf4j
public class AudioServiceApplication {
    public static void main(String[] args) {SpringApplication.run(AudioServiceApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkKafkaTopics() {
        System.out.println("=== CHECKING KAFKA AUTOCONFIGURATION ===");
        try {
            Class.forName("ru.polyrythms.kafka.config.KafkaAutoConfiguration");
            System.out.println("✅ KafkaAutoConfiguration found in classpath");
        } catch (ClassNotFoundException e) {
            System.out.println("❌ KafkaAutoConfiguration NOT found in classpath");
        }
    }
}
