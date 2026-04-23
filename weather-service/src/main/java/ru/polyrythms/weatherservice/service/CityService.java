package ru.polyrythms.weatherservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.polyrythms.weatherservice.dto.CityDto;
import ru.polyrythms.weatherservice.repository.CityMappingRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityMappingRepository repository;

    public List<CityDto> getCitiesByIds(List<Long> cityIds) {
        if (cityIds == null || cityIds.isEmpty()) return List.of();
        return repository.findAllByCityIdIn(cityIds).stream()
                .map(entity -> new CityDto(entity.getCityId(), entity.getCityName()))
                .collect(Collectors.toList());
    }

    public String getCityNameById(Long cityId) {
        return repository.findByCityId(cityId)
                .map(entity -> entity.getCityName())
                .orElse(null);
    }
}