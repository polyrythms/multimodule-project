package ru.polyrythms.telegrambot.infrastructure.adapter.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.polyrythms.telegrambot.application.port.output.CityRepository;
import ru.polyrythms.telegrambot.domain.model.City;
import ru.polyrythms.telegrambot.infrastructure.entity.CityEntity;
import ru.polyrythms.telegrambot.infrastructure.repository.CityJpaRepository;
import ru.polyrythms.telegrambot.infrastructure.adapter.mapper.CityMapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class CityRepositoryImpl implements CityRepository {
    private final CityJpaRepository jpaRepository;
    private final CityMapper mapper;

    @Override
    public List<City> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<City> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<City> findByName(String name) {
        return jpaRepository.findByNameIgnoreCase(name).map(mapper::toDomain);
    }

    @Override
    public City save(City city) {
        CityEntity entity = mapper.toEntity(city);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public void delete(City city) {
        jpaRepository.deleteById(city.getId());
    }
}