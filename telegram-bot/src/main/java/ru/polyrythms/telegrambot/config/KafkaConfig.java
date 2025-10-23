package ru.polyrythms.telegrambot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import ru.polyrythms.kafka.config.KafkaCommonConfig;
import ru.polyrythms.kafka.dto.AudioDecryptionResult;
import ru.polyrythms.kafka.dto.AudioDecryptionTask;

@Configuration
public class KafkaConfig extends KafkaCommonConfig {

    @Bean
    public KafkaTemplate<String, AudioDecryptionTask> audioDecryptionTaskKafkaTemplate() {
        return new KafkaTemplate<>(createProducerFactory(AudioDecryptionTask.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AudioDecryptionResult>
    audioDecryptionResultConcurrentKafkaListenerContainerFactory() {
        return createListenerContainerFactory(AudioDecryptionResult.class, "telegram-bot-group");
    }
}
