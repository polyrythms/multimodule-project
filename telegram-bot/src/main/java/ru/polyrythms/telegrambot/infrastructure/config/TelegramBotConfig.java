// src/main/java/ru/polyrythms/telegrambot/infrastructure/config/TelegramBotConfig.java

package ru.polyrythms.telegrambot.infrastructure.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.polyrythms.telegrambot.infrastructure.adapter.input.telegram.TelegramBotAdapter;

/**
 * Конфигурация Telegram бота.
 * Содержит только статическую конфигурацию:
 * - botName (username) - из проперти
 * - botToken - из проперти
 * botId получается динамически через API при старте бота,
 * поэтому не хранится в проперти.
 */
@Slf4j
@Configuration
@Getter
public class TelegramBotConfig {

    @Value("${telegram.bot.username}")
    private String botName;

    @Value("${telegram.bot.token}")
    private String botToken;

    /**
     * Регистрация бота в Telegram API.
     * Используется новый адаптер TelegramBotAdapter.
     */
    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBotAdapter botAdapter) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(botAdapter);
        log.info("Telegram bot registered: {}", botName);
        return botsApi;
    }
}