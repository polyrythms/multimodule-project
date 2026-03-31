package ru.polyrythms.telegrambot.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.polyrythms.telegrambot.application.dto.TelegramUpdateDto;
import ru.polyrythms.telegrambot.application.port.input.CommandHandlingUseCase;
import ru.polyrythms.telegrambot.application.port.input.GroupManagementUseCase;
import ru.polyrythms.telegrambot.application.port.input.TelegramInboundPort;
import ru.polyrythms.telegrambot.application.port.input.VoiceMessageProcessingUseCase;
import ru.polyrythms.telegrambot.application.port.output.MessageSender;
import ru.polyrythms.telegrambot.infrastructure.metrics.BotMetrics;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Реализация inbound порта для Telegram.
 * Содержит всю бизнес-логику маршрутизации сообщений.
 * Этот класс находится в application слое и не должен зависеть от
 * деталей реализации Telegram API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramUpdateHandler implements TelegramInboundPort {

    private final VoiceMessageProcessingUseCase voiceProcessingUseCase;
    private final CommandHandlingUseCase commandHandlingUseCase;
    private final GroupManagementUseCase groupManagementUseCase;
    private final MessageSender messageSender;
    private final BotMetrics botMetrics;

    // Кэш для разрешенных групп (оптимизация производительности)
    private final ConcurrentHashMap<Long, Boolean> groupCache = new ConcurrentHashMap<>();

    // Кэш для уведомлений об ограничениях (чтобы не спамить)
    private final ConcurrentHashMap<Long, Long> notificationCache = new ConcurrentHashMap<>();

    private static final long NOTIFICATION_COOLDOWN_MS = 3600000; // 1 час

    @Override
    public void handleUpdate(TelegramUpdateDto update) {
        if (update == null) {
            log.warn("Received null update");
            return;
        }

        var timer = botMetrics.startTimer();

        try {
            log.debug("Handling update: userId={}, chatId={}, hasVoice={}, isCommand={}",
                    update.getUserId(), update.getChatId(), update.isHasVoice(), update.isCommand());

            botMetrics.recordMessage();

            // Маршрутизация по типу сообщения
            if (update.isHasVoice()) {
                handleVoiceMessage(update);
            } else if (update.isCommand()) {
                handleCommand(update);
            } else if (update.isHasText()) {
                handlePlainText(update);
            }

        } catch (Exception e) {
            log.error("Error handling update from user: {}", update.getUserId(), e);
            botMetrics.recordError();

            // Отправляем пользователю сообщение об ошибке (только в личных чатах)
            if (!update.isGroupChat()) {
                messageSender.sendMessageAsync(update.getChatId(),
                        "❌ Произошла ошибка при обработке запроса. Пожалуйста, попробуйте позже.");
            }
        } finally {
            botMetrics.stopTimer(timer);
        }
    }

    @Override
    public void handleVoiceMessage(TelegramUpdateDto voiceUpdate) {
        log.info("Processing voice message from userId: {} in chatId: {}",
                voiceUpdate.getUserId(), voiceUpdate.getChatId());

        botMetrics.recordVoiceMessage();

        // Проверка разрешения группы (для групповых чатов)
        if (voiceUpdate.isGroupChat() && !isGroupAllowedCached(voiceUpdate.getChatId())) {
            log.debug("Group chat {} is not allowed for voice messages", voiceUpdate.getChatId());

            if (shouldNotifyAboutRestriction(voiceUpdate.getChatId())) {
                messageSender.sendMessageAsync(voiceUpdate.getChatId(),
                        "⚠️ Этот чат не активирован для обработки голосовых сообщений.\n" +
                                "Обратитесь к администратору для активации.");
            }
            return;
        }

        // Конвертируем DTO в доменную модель и передаем в use case
        voiceProcessingUseCase.processVoiceMessage(voiceUpdate.toVoiceMessage());
    }

    @Override
    public void handleCommand(TelegramUpdateDto commandUpdate) {
        log.info("Processing command: {} from user: {} in chat: {}",
                commandUpdate.getCommandName(), commandUpdate.getUserId(), commandUpdate.getChatId());

        botMetrics.recordCommand(commandUpdate.getCommandName());

        // Конвертируем в CommandContext и передаем в use case
        commandHandlingUseCase.handleCommand(
                commandUpdate.getChatId(),
                commandUpdate.getUserId(),
                commandUpdate.getCommandName(),
                commandUpdate.getCommandArgs(),
                commandUpdate.getText()
        );
    }

    @Override
    public void handlePlainText(TelegramUpdateDto textUpdate) {
        log.debug("Received plain text from user: {} in chat: {}",
                textUpdate.getUserId(), textUpdate.getChatId());

        // Здесь можно добавить обработку обычных текстовых сообщений
        // Например, поиск города для погоды, NLP, etc.

        // Пока просто игнорируем или отправляем подсказку (только в личных чатах)
        if (!textUpdate.isGroupChat() && textUpdate.getText() != null) {
            messageSender.sendMessageAsync(textUpdate.getChatId(),
                    """
                            🤖 Я понимаю только команды.
                            
                            📝 Используйте /help для списка команд.
                            🎤 Или отправьте голосовое сообщение для распознавания.""");
        }
    }

    /**
     * Проверка разрешения группы с кэшированием
     */
    private boolean isGroupAllowedCached(Long chatId) {
        return groupCache.computeIfAbsent(chatId, groupManagementUseCase::isGroupAllowed);
    }

    /**
     * Проверка, нужно ли уведомлять об ограничении (чтобы не спамить)
     */
    private boolean shouldNotifyAboutRestriction(Long chatId) {
        Long lastNotification = notificationCache.get(chatId);
        long now = System.currentTimeMillis();

        if (lastNotification == null || (now - lastNotification) > NOTIFICATION_COOLDOWN_MS) {
            notificationCache.put(chatId, now);
            return true;
        }
        return false;
    }

    // ========== Публичные методы для управления кэшем ==========

    /**
     * Инвалидация кэша для конкретной группы
     */
    public void invalidateGroupCache(Long chatId) {
        groupCache.remove(chatId);
        log.info("Group cache invalidated for chatId: {}", chatId);
    }

    /**
     * Очистка всех кэшей
     */
    public void clearCaches() {
        groupCache.clear();
        notificationCache.clear();
        log.info("All caches cleared");
    }
}