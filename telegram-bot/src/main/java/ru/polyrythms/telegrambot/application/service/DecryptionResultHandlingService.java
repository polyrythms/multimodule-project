package ru.polyrythms.telegrambot.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.polyrythms.telegrambot.application.port.input.DecryptionResultHandlingUseCase;
import ru.polyrythms.telegrambot.application.port.output.MessageSender;

@Slf4j
@RequiredArgsConstructor
public class DecryptionResultHandlingService implements DecryptionResultHandlingUseCase {

    private final MessageSender messageSender;

    @Override
    public void handleDecryptionResult(String taskId, String status, String decryptedText, String errorMessage, Long chatId) {
        log.info("Handling decryption result for taskId: {}, status: {}", taskId, status);

        String responseMessage = buildResponseMessage(status, decryptedText, errorMessage);
        messageSender.sendMessage(chatId, responseMessage);
    }

    private String buildResponseMessage(String status, String decryptedText, String errorMessage) {
        return switch (status) {
            case "SUCCESSFULLY_DECRYPTED" ->
                    "✅ Результат расшифровки:\n" + decryptedText;

            case "PARTIALLY_DECRYPTED" ->
                    "⚠️ Частично расшифровано:\n" + decryptedText +
                            (errorMessage != null ? "\n\nПримечание: " + errorMessage : "");

            case "DECRYPTION_FAILED" ->
                    "❌ Не удалось расшифровать сообщение." +
                            (errorMessage != null ? "\nПричина: " + errorMessage :
                                    "\nПопробуйте записать сообщение еще раз.");

            default -> "❌ Неизвестный статус обработки";
        };
    }
}