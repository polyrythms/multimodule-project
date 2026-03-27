package ru.polyrythms.telegrambot.infrastructure.adapter.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import ru.polyrythms.kafka.Topics;
import ru.polyrythms.kafka.dto.AudioDecryptionResult;
import ru.polyrythms.telegrambot.application.port.input.DecryptionResultHandlingUseCase;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaResultListener {

    private final DecryptionResultHandlingUseCase resultHandlingUseCase;

    @KafkaListener(
            topics = Topics.AUDIO_DECRYPTION_RESULTS,
            containerFactory = "audioDecryptionResultConcurrentKafkaListenerContainerFactory"
    )
    public void handleDecryptionResult(@Payload AudioDecryptionResult result, Acknowledgment ack) {
        log.info("Received decryption result for taskId: {}, status: {}",
                result.getTaskId(), result.getStatus());

        try {
            resultHandlingUseCase.handleDecryptionResult(
                    result.getTaskId(),
                    result.getStatus().name(),
                    result.getDecryptedText(),
                    result.getErrorMessage(),
                    result.getChatId()
            );
            ack.acknowledge();
            log.info("Successfully processed result for taskId: {}", result.getTaskId());
        } catch (Exception e) {
            log.error("Failed to process decryption result for taskId: {}", result.getTaskId(), e);
        }
    }
}