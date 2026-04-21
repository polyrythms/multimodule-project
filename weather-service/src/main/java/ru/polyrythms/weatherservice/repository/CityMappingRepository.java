package ru.polyrythms.weatherservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.polyrythms.weatherservice.entity.CityMappingEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityMappingRepository extends JpaRepository<CityMappingEntity, Long> {
    Optional<CityMappingEntity> findByCityId(Long cityId);
    List<CityMappingEntity> findAllByCityIdIn(List<Long> cityIds);
    Optional<CityMappingEntity> findByCityNameIgnoreCase(String cityName);
}