package ru.polyrythms.telegrambot.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.polyrythms.telegrambot.bot.VoiceMessageBot;

@Configuration
@Getter
public class TelegramBotConfig {
    @Value("${telegram.bot.username}")
    private String botName;
    @Value("${telegram.bot.token}")
    private String botToken;

    @Bean
    public TelegramBotsApi botApiRegister(VoiceMessageBot voiceMessageBot) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(voiceMessageBot);
        return botsApi;
    }
}