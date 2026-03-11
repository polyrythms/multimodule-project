package ru.polyrythms.telegrambot.application.port.input;

public interface DecryptionResultHandlingUseCase {
    void handleDecryptionResult(String taskId, String status, String decryptedText, String errorMessage, Long chatId);
}