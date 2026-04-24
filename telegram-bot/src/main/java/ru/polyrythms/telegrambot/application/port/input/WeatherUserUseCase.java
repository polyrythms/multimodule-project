package ru.polyrythms.telegrambot.application.port.input;

import java.util.Map;

public interface WeatherUserUseCase {
    String authenticateWithInitData(String initData);
    boolean verifyInitData(String initData);
    Map<String, String> parseInitDataParams(String initData);
    Long extractUserId(Map<String, String> params);
    Long extractChatId(Map<String, String> params);

    @Deprecated
    String generateWeatherCode(Long userId, Long chatId);
    @Deprecated
    String exchangeCode(String code, String initData);
}