package ru.polyrythms.telegrambot.application.port.output;

public interface MessageSender {
    void sendMessage(Long chatId, String text);

    // Добавляем асинхронную отправку
    default void sendMessageAsync(Long chatId, String text) {
        sendMessage(chatId, text); // Базовая реализация
    }
}