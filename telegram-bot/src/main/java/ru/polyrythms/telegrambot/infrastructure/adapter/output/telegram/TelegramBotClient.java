package ru.polyrythms.telegrambot.infrastructure.adapter.output.telegram;

import lombok.extern.slf4j.Slf4j;
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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Outbound Client для низкоуровневой коммуникации с Telegram API.
 *
 * Ответственность:
 * 1. Низкоуровневая коммуникация с Telegram API
 * 2. Скачивание файлов
 * 3. Отправка сообщений (синхронно и асинхронно)
 * 4. Получение информации о боте (username, id)
 * 5. Управление пулом потоков для исходящих запросов
 *
 * Этот класс является технической инфраструктурой и не должен содержать
 * бизнес-логики. Он используется outbound адаптерами (MessageSender, FileDownloader).
 *
 * Аналог: RestTemplate, KafkaTemplate, MinioClient
 */
@Slf4j
@Component
public class TelegramBotClient {

    private final TelegramBotConfig config;
    private final DefaultAbsSender bot;
    private final ExecutorService executorService;
    private String botUsername;
    private Long botId;

    public TelegramBotClient(TelegramBotConfig config) {
        this.config = config;

        // Создаем ThreadFactory для исходящих запросов
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicLong threadNumber = new AtomicLong(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("telegram-client-" + threadNumber.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };

        // Пул для исходящих запросов (меньше, чем для входящих)
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                threadFactory
        );

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
        return CompletableFuture.supplyAsync(() -> {
            try {
                return bot.execute(method);
            } catch (TelegramApiException e) {
                log.error("Async execution failed", e);
                throw new RuntimeException("Async execution failed", e);
            }
        }, executorService);
    }

    // ========== GETTERS ==========

    public String getBotUsername() {
        return botUsername;
    }

    public Long getBotId() {
        return botId;
    }

    public DefaultAbsSender getBot() {
        return bot;
    }

    // ========== SHUTDOWN ==========

    @PreDestroy
    public void destroy() {
        log.info("Shutting down TelegramBotClient...");
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
        log.info("TelegramBotClient shut down");
    }
}