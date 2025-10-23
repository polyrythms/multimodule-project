package ru.polyrythms.audioservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.polyrythms.kafka.Topics;
import ru.polyrythms.kafka.dto.AudioDecryptionResult;
import ru.polyrythms.kafka.dto.AudioDecryptionTask;
import ru.polyrythms.kafka.service.BaseKafkaProducer;

@Slf4j
@Service
public class AudioResultProducer extends BaseKafkaProducer<AudioDecryptionResult> {

    public AudioResultProducer(
            KafkaTemplate<String, AudioDecryptionResult> kafkaTemplate) {
        super(kafkaTemplate, Topics.AUDIO_DECRYPTION_RESULTS);
    }

    public Mono<Void> sendSuccessResult(AudioDecryptionTask task, String transcription) {
        AudioDecryptionResult result = AudioDecryptionResult.createSuccessResult(
                task.getTaskId(),
                task.getAudioId(), task.getChatId(),
                transcription);

        return send(task.getAudioId(), result)
                .doOnSuccess(v -> log.info("Success result sent for task: {}", task.getTaskId()))
                .doOnError(e -> log.error("Failed to send success result for task: {}", task.getTaskId(), e));
    }

    public Mono<Void> sendErrorResult(AudioDecryptionTask task, String errorMessage) {
        AudioDecryptionResult result = AudioDecryptionResult.createFailedResult(
                task.getTaskId(),
                task.getAudioId(), task.getChatId(),
                errorMessage);

        return send(task.getAudioId(), result)
                .doOnSuccess(v -> log.info("Error result sent for task: {}", task.getTaskId()))
                .doOnError(e -> log.error("Failed to send error result for task: {}", task.getTaskId(), e));
    }
}