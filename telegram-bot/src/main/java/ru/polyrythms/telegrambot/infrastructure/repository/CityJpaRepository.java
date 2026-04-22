package ru.polyrythms.telegrambot.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.polyrythms.telegrambot.infrastructure.entity.CityEntity;
import java.util.Optional;

public interface CityJpaRepository extends JpaRepository<CityEntity, Long> {
    Optional<CityEntity> findByNameIgnoreCase(String name);
}