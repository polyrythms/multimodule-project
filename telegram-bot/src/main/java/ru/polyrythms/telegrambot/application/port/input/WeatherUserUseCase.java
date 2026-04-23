package ru.polyrythms.telegrambot.application.port.input;

public interface WeatherUserUseCase {
    String generateWeatherCode(Long userId, Long chatId);
    String exchangeCode(String code, String initData);
}