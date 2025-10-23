package ru.polyrythms.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioDecryptionResult implements Serializable {
    private String taskId;
    private String audioId;
    private Long chatId;
    private String decryptedText;
    private DecryptionStatus status;
    private Long processedAt;
    private String errorMessage;

    // Конвертеры для удобства работы с датами
    public Instant getProcessedAtAsInstant() {
        return processedAt != null ? Instant.ofEpochMilli(processedAt) : null;
    }


    // Статические фабричные методы
    public static AudioDecryptionResult createSuccessResult(String taskId, String audioId, Long chatId, String decryptedText) {
        return AudioDecryptionResult.builder()
                .taskId(taskId)
                .audioId(audioId)
                .chatId(chatId)
                .decryptedText(decryptedText)
                .status(DecryptionStatus.SUCCESSFULLY_DECRYPTED)
                .processedAt(System.currentTimeMillis())
                .build();
    }

    public static AudioDecryptionResult createFailedResult(String taskId, String audioId, Long chatId, String errorMessage) {
        return AudioDecryptionResult.builder()
                .taskId(taskId)
                .audioId(audioId)
                .chatId(chatId)
                .status(DecryptionStatus.DECRYPTION_FAILED)
                .processedAt(System.currentTimeMillis())
                .errorMessage(errorMessage)
                .build();
    }

    public static AudioDecryptionResult createPartialResult(String taskId, String audioId, Long chatId, String decryptedText, Double confidenceScore, String errorMessage) {
        return AudioDecryptionResult.builder()
                .taskId(taskId)
                .audioId(audioId)
                .chatId(chatId)
                .decryptedText(decryptedText)
                .status(DecryptionStatus.PARTIALLY_DECRYPTED)
                .processedAt(System.currentTimeMillis())
                .errorMessage(errorMessage)
                .build();
    }

    public enum DecryptionStatus {
        SUCCESSFULLY_DECRYPTED, DECRYPTION_FAILED, PARTIALLY_DECRYPTED
    }
}