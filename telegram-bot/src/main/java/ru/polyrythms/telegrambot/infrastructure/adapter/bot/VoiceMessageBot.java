// infrastructure/adapter/bot/VoiceMessageBot.java
package ru.polyrythms.telegrambot.infrastructure.adapter.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.polyrythms.telegrambot.application.port.input.CommandHandlingUseCase;
import ru.polyrythms.telegrambot.application.port.input.GroupManagementUseCase;
import ru.polyrythms.telegrambot.application.port.input.VoiceMessageProcessingUseCase;
import ru.polyrythms.telegrambot.domain.model.VoiceMessage;
import ru.polyrythms.telegrambot.infrastructure.config.TelegramBotConfig;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class VoiceMessageBot extends TelegramLongPollingBot {

    private final TelegramBotConfig config;
    private final TelegramBotWrapper botWrapper;
    private final VoiceMessageProcessingUseCase voiceProcessingUseCase;
    private final CommandHandlingUseCase commandHandlingUseCase;
    private final GroupManagementUseCase groupManagementUseCase;
    private final ExecutorService executorService;

    // Кэш для разрешенных групп
    private final java.util.concurrent.ConcurrentHashMap<Long, Boolean> groupCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    public VoiceMessageBot(
            TelegramBotConfig config,
            TelegramBotWrapper botWrapper,
            VoiceMessageProcessingUseCase voiceProcessingUseCase,
            CommandHandlingUseCase commandHandlingUseCase,
            GroupManagementUseCase groupManagementUseCase) {
        super(config.getBotToken());
        this.config = config;
        this.botWrapper = botWrapper;
        this.voiceProcessingUseCase = voiceProcessingUseCase;
        this.commandHandlingUseCase = commandHandlingUseCase;
        this.groupManagementUseCase = groupManagementUseCase;

        // Создаем пул потоков для обработки сообщений
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicLong threadNumber = new AtomicLong(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("tg-update-" + threadNumber.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            }
        };

        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 2,
                threadFactory
        );

        log.info("VoiceMessageBot initialized with executor pool size: {}",
                Runtime.getRuntime().availableProcessors() * 2);
    }

    @Override
    public String getBotUsername() {
        return botWrapper.getBotUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update == null || !update.hasMessage()) {
            return;
        }

        // Асинхронная обработка
        executorService.submit(() -> processUpdate(update));
    }

    private void processUpdate(Update update) {
        long startTime = System.currentTimeMillis();

        try {
            Message message = update.getMessage();

            // Валидация сообщения - проверяем через getFrom() и getChat() на null
            if (!isValidMessage(message)) {
                return;
            }

            Long chatId = message.getChatId();
            Long userId = message.getFrom().getId();
            boolean isGroupChat = message.getChat().isGroupChat() ||
                    message.getChat().isSuperGroupChat();

            // Логируем входящее сообщение
            log.debug("Processing update: userId={}, chatId={}, isGroup={}, hasVoice={}, hasText={}",
                    userId, chatId, isGroupChat, message.hasVoice(),
                    message.hasText() ? message.getText() : "null");

            // Обработка команд
            if (message.hasText() && message.getText().startsWith("/")) {
                handleCommand(message, chatId, userId);
                return;
            }

            // Обработка голосовых сообщений
            if (message.hasVoice()) {
                handleVoiceMessage(message, chatId, userId, isGroupChat);
            }

        } catch (Exception e) {
            log.error("Error processing update", e);
        } finally {
            long processingTime = System.currentTimeMillis() - startTime;
            if (processingTime > 1000) {
                log.warn("Slow processing: {} ms", processingTime);
            }
        }
    }

    /**
     * Проверка валидности сообщения
     */
    private boolean isValidMessage(Message message) {
        // Проверяем наличие отправителя
        if (message.getFrom() == null) {
            log.warn("Invalid message: missing from");
            return false;
        }

        // Проверяем наличие чата
        if (message.getChat() == null) {
            log.warn("Invalid message: missing chat");
            return false;
        }

        // Игнорируем сообщения от самого бота
        Long botId = botWrapper.getBotId();
        if (botId != null && message.getFrom().getId().equals(botId)) {
            return false;
        }

        return true;
    }

    private void handleVoiceMessage(Message message,
                                    Long chatId,
                                    Long userId,
                                    boolean isGroupChat) {
        // Проверка разрешения группы (с кэшированием)
        if (isGroupChat && !isGroupAllowedCached(chatId)) {
            log.debug("Group chat {} is not allowed", chatId);
            return;
        }

        VoiceMessage voiceMessage = VoiceMessage.builder()
                .fileId(message.getVoice().getFileId())
                .fileUniqueId(message.getVoice().getFileUniqueId())
                .chatId(chatId)
                .userId(userId)
                .duration(message.getVoice().getDuration())
                .isGroupChat(isGroupChat)
                .build();

        voiceProcessingUseCase.processVoiceMessage(voiceMessage);
    }

    private void handleCommand(Message message, Long chatId, Long userId) {
        String text = message.getText();

        // Очищаем команду от @botname если есть
        String command = text.split("\\s+")[0];
        if (command.contains("@")) {
            command = command.split("@")[0];
        }

        String[] args = text.split("\\s+");

        commandHandlingUseCase.handleCommand(chatId, userId, command, args, text);
    }

    /**
     * Проверка разрешения группы с кэшированием
     * Используем method reference вместо лямбды
     */
    private boolean isGroupAllowedCached(Long chatId) {
        return groupCache.computeIfAbsent(chatId, groupManagementUseCase::isGroupAllowed);
    }

    @PreDestroy
    public void destroy() {
        log.info("Shutting down VoiceMessageBot...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        groupCache.clear();
        log.info("VoiceMessageBot shut down");
    }
}