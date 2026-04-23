package ru.polyrythms.telegrambot.infrastructure.adapter.mapper;

import org.springframework.stereotype.Component;
import ru.polyrythms.telegrambot.domain.model.City;
import ru.polyrythms.telegrambot.infrastructure.entity.CityEntity;

@Component
public class CityMapper {
    public City toDomain(CityEntity entity) {
        if (entity == null) return null;
        return City.builder().id(entity.getId()).name(entity.getName()).build();
    }
    public CityEntity toEntity(City domain) {
        CityEntity entity = new CityEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        return entity;
    }
}