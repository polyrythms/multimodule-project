package ru.polyrythms.audioservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.polyrythms.kafka.Topics;
import ru.polyrythms.kafka.dto.AudioDecryptionTask;

@Slf4j
@Service
public class AudioTaskConsumer {

    private final ReactiveAssemblyAIService assemblyAIService;
    private final AudioResultProducer resultProducer;

    public AudioTaskConsumer(ReactiveAssemblyAIService assemblyAIService,
                             AudioResultProducer resultProducer) {
        this.assemblyAIService = assemblyAIService;
        this.resultProducer = resultProducer;
    }

    @KafkaListener(
            topics = Topics.AUDIO_DECRYPTION_REQUESTS,
            containerFactory = "audioTaskListenerContainerFactory"
    )
    public void consumeAudioTask(@Payload AudioDecryptionTask task,
                                 @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                                 Acknowledgment ack) {

        log.info("Received audio task: {} for audio: {}, created at: {}",
                task.getTaskId(), task.getAudioId(), task.getCreatedAtAsInstant());

        assemblyAIService.transcribeAudio(task.getAudioId())
                .flatMap(transcription -> resultProducer.sendSuccessResult(task, transcription))
                .doOnSuccess(v -> {
                    ack.acknowledge();
                    log.info("Task processed successfully: {}", task.getTaskId());
                })
                .doOnError(error -> {
                    resultProducer.sendErrorResult(task, error.getMessage())
                            .doOnSuccess(v -> ack.acknowledge())
                            .subscribe();
                    log.error("Task processing failed: {}", task.getTaskId(), error);
                })
                .subscribe();
    }
}