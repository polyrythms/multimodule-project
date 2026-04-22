package ru.polyrythms.telegrambot.application.port.input;

import ru.polyrythms.telegrambot.domain.model.City;
import java.util.List;

public interface WeatherAdminUseCase {
    City addCity(String name);
    List<City> listCities();
    void assignCityToGroup(Long groupChatId, Long cityId, Long adminId);
    void removeCityFromGroup(Long groupChatId, Long cityId, Long adminId);
    List<City> getCitiesForGroup(Long groupChatId);
}