package ru.polyrythms.telegrambot.infrastructure.adapter.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.polyrythms.telegrambot.application.port.output.MessageSender;

@Slf4j
@Component
public class TelegramMessageSender implements MessageSender {

    private final DefaultAbsSender telegramBot;

    public TelegramMessageSender(@Lazy DefaultAbsSender telegramBot) {
        this.telegramBot = telegramBot;
    }

    @Override
    @SneakyThrows
    public void sendMessage(Long chatId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(chatId.toString());
            message.setText(text);
            telegramBot.execute(message);
            log.debug("Message sent to chatId: {}", chatId);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to chatId: {}", chatId, e);
            throw e;
        }
    }
}