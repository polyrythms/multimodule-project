package ru.polyrythms.telegrambot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import ru.polyrythms.kafka.Topics;
import ru.polyrythms.kafka.dto.AudioDecryptionResult;
import ru.polyrythms.telegrambot.bot.VoiceMessageBot;

@Slf4j
@Service
public class ResultProcessingService {

    private final VoiceMessageBot voiceMessageBot;

    public ResultProcessingService(VoiceMessageBot voiceMessageBot) {
        this.voiceMessageBot = voiceMessageBot;
    }

    @KafkaListener(
            topics = Topics.AUDIO_DECRYPTION_RESULTS,
            containerFactory = "audioDecryptionResultConcurrentKafkaListenerContainerFactory"
    )
    public void handleDecryptionResult(@Payload AudioDecryptionResult result, Acknowledgment ack) {
        log.info("Received decryption result for taskId: {}, status: {}",
                result.getTaskId(), result.getStatus());

        try {
            String responseMessage = buildResponseMessage(result);
            voiceMessageBot.sendTextMessage(result.getChatId(), responseMessage);
            ack.acknowledge();
            log.info("Successfully sent response to chatId: {}", result.getChatId());
        } catch (Exception e) {
            log.error("Failed to process decryption result for taskId: {}", result.getTaskId(), e);
        }
    }

    private String buildResponseMessage(AudioDecryptionResult result) {
        return switch (result.getStatus()) {
            case SUCCESSFULLY_DECRYPTED ->
                    "✅ Результат расшифровки:\n" + result.getDecryptedText();

            case PARTIALLY_DECRYPTED ->
                    "⚠️ Частично расшифровано:\n" + result.getDecryptedText() +
                            (result.getErrorMessage() != null ?
                                    "\n\nПримечание: " + result.getErrorMessage() : "");

            case DECRYPTION_FAILED ->
                    "❌ Не удалось расшифровать сообщение." +
                            (result.getErrorMessage() != null ?
                                    "\nПричина: " + result.getErrorMessage() :
                                    "\nПопробуйте записать сообщение еще раз.");
        };
    }
}