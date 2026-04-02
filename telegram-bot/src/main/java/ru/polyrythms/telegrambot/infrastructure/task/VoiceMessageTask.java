package ru.polyrythms.telegrambot.infrastructure.task;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.polyrythms.telegrambot.application.dto.TelegramUpdateDto;
import ru.polyrythms.telegrambot.application.port.input.TelegramInboundPort;
import ru.polyrythms.telegrambot.application.port.output.MessageSender;

@Slf4j
@RequiredArgsConstructor
public class VoiceMessageTask implements Runnable {

    @Getter
    private final Long chatId;
    private final TelegramUpdateDto update;
    private final TelegramInboundPort inboundPort;
    private final MessageSender messageSender;
    private final long createdAt = System.currentTimeMillis();

    @Override
    public void run() {
        long waitTime = System.currentTimeMillis() - createdAt;
        if (waitTime > 5000) {
            log.warn("Task started after {} ms wait time for chatId: {}", waitTime, chatId);
        }

        try {
            inboundPort.handleUpdate(update);
        } catch (Exception e) {
            log.error("Error processing voice message for chatId: {}", chatId, e);
            sendErrorMessage();
        }
    }

    public void sendOverloadNotification() {
        String message = """
                ⚠️ *Сервер перегружен*
                
                Ваше сообщение не может быть обработано сейчас.
                Пожалуйста, попробуйте отправить его через несколько минут.
                
                Приносим извинения за неудобства.""";

        messageSender.sendMessageAsync(chatId, message);
    }

    private void sendErrorMessage() {
        String message = """
                ❌ *Ошибка обработки*
                
                Не удалось обработать ваше голосовое сообщение.
                Пожалуйста, попробуйте еще раз.""";

        messageSender.sendMessageAsync(chatId, message);
    }
}