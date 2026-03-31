package ru.polyrythms.telegrambot.infrastructure.adapter.input.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.polyrythms.telegrambot.application.dto.TelegramUpdateDto;
import ru.polyrythms.telegrambot.application.port.input.TelegramInboundPort;
import ru.polyrythms.telegrambot.application.port.output.MessageSender;
import ru.polyrythms.telegrambot.infrastructure.config.TelegramBotConfig;
import ru.polyrythms.telegrambot.infrastructure.metrics.BotMetrics;
import ru.polyrythms.telegrambot.infrastructure.task.VoiceMessageTask;

import jakarta.annotation.PreDestroy;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TelegramBotAdapter extends TelegramLongPollingBot {

    private final TelegramBotConfig config;
    private final TelegramInboundPort inboundPort;
    private final MessageSender messageSender;
    private final BotMetrics botMetrics;
    private final ExecutorService executorService;
    private final Long botId;  // final поле

    public TelegramBotAdapter(
            TelegramBotConfig config,
            TelegramInboundPort inboundPort,
            MessageSender messageSender,
            BotMetrics botMetrics,
            ExecutorService telegramInboundExecutor) {
        super(config.getBotToken());
        this.config = config;
        this.inboundPort = inboundPort;
        this.messageSender = messageSender;
        this.botMetrics = botMetrics;
        this.executorService = telegramInboundExecutor;

        // Инициализация ID бота при создании
        this.botId = initializeBotId();

        log.info("TelegramBotAdapter initialized with botId: {}", botId);
    }

    private Long initializeBotId() {
        try {
            var me = execute(new GetMe());
            Long id = me.getId();
            log.info("Bot ID obtained: {}", id);
            return id;
        } catch (TelegramApiException e) {
            log.error("Failed to get bot ID", e);
            return null;
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update == null || !update.hasMessage()) {
            return;
        }

        Long userId = update.getMessage().getFrom().getId();

        // Самый безопасный способ
        if (Objects.equals(userId, botId)) {
            log.debug("Ignoring message from self");
            return;
        }

        // Создаем DTO
        TelegramUpdateDto dto = TelegramUpdateDto.fromUpdate(update);
        if (dto == null) {
            log.warn("Could not create DTO from update");
            return;
        }

        // Создаем и отправляем задачу
        VoiceMessageTask task = new VoiceMessageTask(
                dto.getChatId(),
                dto,
                inboundPort,
                messageSender
        );

        try {
            executorService.execute(task);
            botMetrics.recordTaskSubmitted();
        } catch (Exception e) {
            log.error("Failed to submit task for chatId: {}", dto.getChatId(), e);
            botMetrics.recordTaskRejected();
            messageSender.sendMessageAsync(dto.getChatId(),
                    "⚠️ *Сервер перегружен*\n\nПожалуйста, попробуйте позже.");
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("Shutting down TelegramBotAdapter...");

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Forcing shutdown after timeout");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("TelegramBotAdapter shut down");
    }
}