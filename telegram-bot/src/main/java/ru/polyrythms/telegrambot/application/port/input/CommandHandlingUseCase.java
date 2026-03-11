package ru.polyrythms.telegrambot.application.port.input;

public interface CommandHandlingUseCase {
    void handleCommand(Long chatId, Long userId, String command, String[] args, String fullText);
}