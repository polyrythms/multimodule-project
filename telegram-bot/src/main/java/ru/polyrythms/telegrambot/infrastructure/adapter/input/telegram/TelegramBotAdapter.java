package ru.polyrythms.telegrambot.infrastructure.adapter.input.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.polyrythms.telegrambot.application.dto.TelegramUpdateDto;
import ru.polyrythms.telegrambot.application.port.input.TelegramInboundPort;
import ru.polyrythms.telegrambot.infrastructure.config.TelegramBotConfig;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Inbound Adapter для Telegram API.
 * Ответственность:
 * 1. Подключение к Telegram API через Long Polling
 * 2. Прием входящих Update
 * 3. Асинхронная диспетчеризация через пул потоков
 * 4. Конвертация Update → TelegramUpdateDto
 * 5. Вызов inbound порта
 * НЕ СОДЕРЖИТ:
 * - Бизнес-логики
 * - Логики маршрутизации команд/голоса
 * - Работы с файлами
 * Это "чистый" адаптер, который только преобразует внешние события
 * во внутренние DTO и передает их в приложение.
 */
@Slf4j
@Component
public class TelegramBotAdapter extends TelegramLongPollingBot {

    private final TelegramBotConfig config;
    private final TelegramInboundPort inboundPort;
    private final ExecutorService executorService;
    private volatile Long botId;

    public TelegramBotAdapter(
            TelegramBotConfig config,
            TelegramInboundPort inboundPort) {
        super(config.getBotToken());
        this.config = config;
        this.inboundPort = inboundPort;

        // Создаем пул потоков для асинхронной обработки входящих сообщений
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicLong threadNumber = new AtomicLong(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("telegram-adapter-" + threadNumber.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };

        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 2,
                threadFactory
        );

        log.info("TelegramBotAdapter initialized with pool size: {}",
                Runtime.getRuntime().availableProcessors() * 2);
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

        // Получаем ID бота для проверки
        Long botId = getBotId();

        // Игнорируем сообщения от самого бота
        if (botId != null && update.getMessage().getFrom().getId().equals(botId)) {
            log.debug("Ignoring message from self, botId: {}", botId);
            return;
        }

        // Асинхронная обработка - не блокируем поток Telegram API
        executorService.submit(() -> processUpdate(update));
    }

    /**
     * Обработка одного update в отдельном потоке
     */
    private void processUpdate(Update update) {
        long startTime = System.currentTimeMillis();

        try {
            // Конвертируем Telegram Update в наш внутренний DTO
            TelegramUpdateDto dto = TelegramUpdateDto.fromUpdate(update);

            if (dto != null) {
                // Вся бизнес-логика - в порте!
                inboundPort.handleUpdate(dto);
            }

        } catch (Exception e) {
            log.error("Error processing update", e);
        } finally {
            long processingTime = System.currentTimeMillis() - startTime;
            if (processingTime > 1000) {
                log.warn("Slow processing: {} ms for update from user: {}",
                        processingTime,
                        update.hasMessage() ? update.getMessage().getFrom().getId() : "unknown");
            }
        }
    }

    /**
     * Получение ID бота с кэшированием.
     * Получаем динамически через API, а не из проперти.
     */
    private Long getBotId() {
        if (botId == null) {
            synchronized (this) {
                if (botId == null) {
                    try {
                        var me = execute(new org.telegram.telegrambots.meta.api.methods.GetMe());
                        botId = me.getId();
                        log.info("Bot ID obtained dynamically: {}", botId);
                    } catch (Exception e) {
                        log.error("Failed to get bot ID", e);
                        return null;
                    }
                }
            }
        }
        return botId;
    }

    @PreDestroy
    public void destroy() {
        log.info("Shutting down TelegramBotAdapter...");

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("ExecutorService did not terminate gracefully, forcing shutdown...");
                executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("ExecutorService did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("TelegramBotAdapter shut down");
    }
}