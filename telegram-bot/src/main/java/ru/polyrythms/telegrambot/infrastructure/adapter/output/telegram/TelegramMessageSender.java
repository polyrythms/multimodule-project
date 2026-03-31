package ru.polyrythms.telegrambot.infrastructure.adapter.output.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.polyrythms.telegrambot.application.port.output.MessageSender;

/**
 * Реализация outbound порта MessageSender для Telegram.
 * Использует TelegramBotClient для низкоуровневой отправки сообщений.
 * Поддерживает синхронную и асинхронную отправку.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramMessageSender implements MessageSender {

    private final TelegramBotClient botClient;

    @Override
    public void sendMessage(Long chatId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(text);
            message.setParseMode("HTML");

            botClient.executeWithErrorHandling(message);
            log.debug("Message sent to chatId: {}", chatId);

        } catch (Exception e) {
            log.error("Failed to send message to chatId: {}", chatId, e);
            throw new RuntimeException("Failed to send message", e);
        }
    }

    @Override
    public void sendMessageAsync(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("HTML");

        botClient.sendMessageAsync(message)
                .thenAccept(result -> log.debug("Async message sent to chatId: {}", chatId))
                .exceptionally(e -> {
                    log.error("Async message failed for chatId: {}", chatId, e);
                    return null;
                });
    }
}