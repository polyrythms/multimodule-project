package ru.polyrythms.telegrambot.infrastructure.adapter.output.telegram;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.polyrythms.telegrambot.infrastructure.config.TelegramBotConfig;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Outbound Client для низкоуровневой коммуникации с Telegram API.
 * <p>
 * Ответственность:
 * 1. Низкоуровневая коммуникация с Telegram API
 * 2. Скачивание файлов
 * 3. Отправка сообщений (синхронно и асинхронно)
 * 4. Получение информации о боте
 */
@Slf4j
@Component
public class TelegramBotClient {

    private final TelegramBotConfig config;
    private final ExecutorService executorService;
    @Getter
    private final DefaultAbsSender bot;
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    @Getter
    private String botUsername;
    @Getter
    private Long botId;

    public TelegramBotClient(
            TelegramBotConfig config,
            @Qualifier("telegramOutboundExecutor") ExecutorService executorService) {
        this.config = config;
        this.executorService = executorService;

        // Инициализация клиента Telegram
        DefaultBotOptions options = new DefaultBotOptions();
        options.setGetUpdatesTimeout(30);
        options.setMaxThreads(10);

        this.bot = new DefaultAbsSender(options) {
            @Override
            public String getBotToken() {
                return config.getBotToken();
            }
        };

        log.info("TelegramBotClient initialized");
    }

    @PostConstruct
    public void init() {
        log.info("Initializing TelegramBotClient...");
        try {
            GetMe getMe = new GetMe();
            User me = bot.execute(getMe);
            this.botUsername = me.getUserName();
            this.botId = me.getId();
            log.info("Telegram bot client initialized: @{} (ID: {})", botUsername, botId);
        } catch (TelegramApiException e) {
            log.error("Failed to get bot info", e);
            this.botUsername = config.getBotName();
            // Не падаем, но логгируем ошибку - бот все равно будет работать
        }
    }

    // ========== FILE OPERATIONS ==========

    /**
     * Получение информации о файле из Telegram
     */
    public File getTelegramFileInfo(String fileId) throws TelegramApiException {
        if (isShuttingDown.get()) {
            throw new IllegalStateException("Client is shutting down");
        }

        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);
        return bot.execute(getFile);
    }

    /**
     * Скачивание файла по пути
     */
    public java.io.File downloadFileByPath(String filePath) throws TelegramApiException {
        if (isShuttingDown.get()) {
            throw new IllegalStateException("Client is shutting down");
        }

        return bot.downloadFile(filePath);
    }

    /**
     * Скачивание файла по ID (удобный метод)
     */
    public java.io.File downloadFileById(String fileId) throws TelegramApiException {
        File fileInfo = getTelegramFileInfo(fileId);
        return downloadFileByPath(fileInfo.getFilePath());
    }

    // ========== MESSAGE SENDING ==========

    /**
     * Синхронное выполнение метода Telegram API
     */
    public <T extends Serializable, M extends BotApiMethod<T>> T execute(M method) {
        if (isShuttingDown.get()) {
            throw new IllegalStateException("Client is shutting down, rejecting new requests");
        }

        try {
            return bot.execute(method);
        } catch (TelegramApiException e) {
            log.error("Telegram API error: {}", e.getMessage(), e);
            throw new RuntimeException("Telegram API error: " + e.getMessage(), e);
        }
    }

    /**
     * Синхронное выполнение с обработкой ошибок (алиас для execute)
     */
    public <T extends Serializable, M extends BotApiMethod<T>> T executeWithErrorHandling(M method) {
        return execute(method);
    }

    /**
     * Асинхронная отправка сообщения
     */
    public CompletableFuture<Serializable> sendMessageAsync(SendMessage message) {
        if (isShuttingDown.get()) {
            CompletableFuture<Serializable> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Client is shutting down"));
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Sending message async to chatId: {}", message.getChatId());
                return bot.execute(message);
            } catch (TelegramApiException e) {
                log.error("Failed to send message async to chatId: {}", message.getChatId(), e);
                throw new RuntimeException("Failed to send message", e);
            }
        }, executorService);
    }

    /**
     * Асинхронное выполнение любого метода
     */
    public CompletableFuture<Serializable> executeMethodAsync(BotApiMethod<?> method) {
        if (isShuttingDown.get()) {
            CompletableFuture<Serializable> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Client is shutting down"));
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return bot.execute(method);
            } catch (TelegramApiException e) {
                log.error("Async execution failed", e);
                throw new RuntimeException("Async execution failed", e);
            }
        }, executorService);
    }

    // ========== SHUTDOWN ==========

    @PreDestroy
    public void destroy() {
        log.info("Shutting down TelegramBotClient...");
        isShuttingDown.set(true);

        executorService.shutdown();
        try {
            log.info("Waiting for pending tasks to complete (max 30 seconds)...");
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("ExecutorService did not terminate gracefully, forcing shutdown...");
                executorService.shutdownNow();

                // Дополнительное ожидание для принудительного завершения
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("ExecutorService did not terminate even after forced shutdown");
                }
            }

            long completedTasks = executorService instanceof java.util.concurrent.ThreadPoolExecutor ?
                    ((java.util.concurrent.ThreadPoolExecutor) executorService).getCompletedTaskCount() : 0;
            log.info("TelegramBotClient shut down. Completed tasks: {}", completedTasks);

        } catch (InterruptedException e) {
            log.warn("Shutdown interrupted", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}