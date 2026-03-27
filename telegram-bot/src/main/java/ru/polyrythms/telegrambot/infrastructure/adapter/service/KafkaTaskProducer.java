package ru.polyrythms.telegrambot.infrastructure.adapter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.polyrythms.kafka.Topics;
import ru.polyrythms.kafka.dto.AudioDecryptionTask;
import ru.polyrythms.telegrambot.application.port.output.DecryptionTaskProducer;
import ru.polyrythms.telegrambot.domain.model.DecryptionTask;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTaskProducer implements DecryptionTaskProducer {

    private final KafkaTemplate<String, AudioDecryptionTask> kafkaTemplate;

    @Override
    public void sendTask(DecryptionTask task) {
        AudioDecryptionTask kafkaTask = AudioDecryptionTask.createVoiceTask(
                task.getAudioId(),
                task.getChatId(),
                task.getAudioUrl()
        );

        kafkaTemplate.send(Topics.AUDIO_DECRYPTION_REQUESTS, task.getTaskId(), kafkaTask);
        log.info("Task sent to Kafka, taskId: {}", task.getTaskId());
    }
}