package ru.polyrythms.telegrambot.infrastructure.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import ru.polyrythms.telegrambot.infrastructure.adapter.bot.TelegramBotWrapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBotHealthIndicator implements HealthIndicator {
    
    private final TelegramBotWrapper botWrapper;
    
    @Override
    public Health health() {
        try {
            // Проверяем, что бот инициализирован
            if (botWrapper.getBotUsername() == null) {
                return Health.down()
                    .withDetail("error", "Bot not initialized")
                    .build();
            }
            
            // Проверяем, что бот может выполнить простой запрос
            var fileInfo = botWrapper.getTelegramFileInfo("test");
            
            return Health.up()
                .withDetail("botUsername", botWrapper.getBotUsername())
                .withDetail("botId", botWrapper.getBotId())
                .build();
                
        } catch (Exception e) {
            log.error("Health check failed", e);
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}