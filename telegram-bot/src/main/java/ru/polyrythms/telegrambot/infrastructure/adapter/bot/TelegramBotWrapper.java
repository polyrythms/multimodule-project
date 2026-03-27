package ru.polyrythms.telegrambot.infrastructure.adapter.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.polyrythms.telegrambot.infrastructure.config.TelegramBotConfig;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class TelegramBotWrapper {

    private final TelegramBotConfig config;
    private final DefaultAbsSender bot;
    private final ExecutorService executorService;
    private String botUsername;
    private Long botId;  // Добавляем поле для ID бота

    // Альтернатива для старых версий библиотеки
    public TelegramBotWrapper(TelegramBotConfig config) {
        this.config = config;

        // Создаем ThreadFactory
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicLong threadNumber = new AtomicLong(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("telegram-bot-" + threadNumber.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };

        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                threadFactory
        );

        // Старый способ создания DefaultBotOptions (не deprecated, но может быть помечен)
        DefaultBotOptions options = new DefaultBotOptions();
        options.setGetUpdatesTimeout(30);
        options.setMaxThreads(10); // Если есть этот метод

        this.bot = new DefaultAbsSender(options) {
            @Override
            public String getBotToken() {
                return config.getBotToken();
            }
        };
    }

    @PostConstruct
    public void init() {
        log.info("Initializing TelegramBotWrapper...");
        try {
            GetMe getMe = new GetMe();
            org.telegram.telegrambots.meta.api.objects.User me = bot.execute(getMe);
            this.botUsername = me.getUserName();
            this.botId = me.getId();  // Сохраняем ID бота
            log.info("Telegram bot initialized: @{} (ID: {})", botUsername, botId);
        } catch (TelegramApiException e) {
            log.error("Failed to get bot info", e);
            this.botUsername = config.getBotName();
        }
    }

    public Long getBotId() {
        return botId;
    }

    @PreDestroy
    public void destroy() {
        log.info("Shutting down TelegramBotWrapper...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("TelegramBotWrapper shut down");
    }

    /**
     * Получение информации о файле из Telegram
     */
    public File getTelegramFileInfo(String fileId) throws TelegramApiException {
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);
        return bot.execute(getFile);
    }

    /**
     * Скачивание файла по пути
     */
    public java.io.File downloadFileByPath(String filePath) throws TelegramApiException {
        return bot.downloadFile(filePath);
    }

    /**
     * Скачивание файла по ID
     */
    public java.io.File downloadFileById(String fileId) throws TelegramApiException {
        File fileInfo = getTelegramFileInfo(fileId);
        return downloadFileByPath(fileInfo.getFilePath());
    }

    /**
     * Синхронное выполнение с обработкой ошибок
     */
    public <T extends Serializable, M extends BotApiMethod<T>>
    T executeWithErrorHandling(M method) {
        try {
            return bot.execute(method);
        } catch (TelegramApiException e) {
            log.error("Telegram API error: {}", e.getMessage(), e);
            throw new RuntimeException("Telegram API error: " + e.getMessage(), e);
        }
    }

    /**
     * Асинхронная отправка сообщения
     */
    public CompletableFuture<Serializable> sendMessageAsync(SendMessage message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return bot.execute(message);
            } catch (TelegramApiException e) {
                log.error("Failed to send message async", e);
                throw new RuntimeException("Failed to send message", e);
            }
        }, executorService);
    }

    /**
     * Асинхронное выполнение любого метода
     */
    public CompletableFuture<Serializable> executeMethodAsync(
            org.telegram.telegrambots.meta.api.methods.BotApiMethod<?> method) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return bot.execute(method);
            } catch (TelegramApiException e) {
                log.error("Async execution failed", e);
                throw new RuntimeException("Async execution failed", e);
            }
        }, executorService);
    }

    public String getBotUsername() {
        return botUsername;
    }

    public DefaultAbsSender getBot() {
        return bot;
    }
}