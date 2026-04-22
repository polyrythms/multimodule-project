package ru.polyrythms.telegrambot.application.port.output;

import ru.polyrythms.telegrambot.domain.model.City;
import java.util.List;
import java.util.Optional;

public interface CityRepository {
    List<City> findAll();
    Optional<City> findById(Long id);
    Optional<City> findByName(String name);
    City save(City city);
    void delete(City city);
}