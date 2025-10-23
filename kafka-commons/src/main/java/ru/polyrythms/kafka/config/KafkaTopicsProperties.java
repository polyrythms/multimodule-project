package ru.polyrythms.kafka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaTopicsProperties {
    private String bootstrapServers = "localhost:9092";
    private int partitions = 6;
    private short replicas = 1;
    private long retentionMs = 604800000L; // 7 дней
    private boolean autoCreate = true;

}