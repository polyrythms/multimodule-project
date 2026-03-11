package ru.polyrythms.telegrambot.application.port.output;

public interface MessageSender {
    void sendMessage(Long chatId, String text);
}