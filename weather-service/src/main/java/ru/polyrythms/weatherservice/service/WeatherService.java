package ru.polyrythms.weatherservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.polyrythms.weatherservice.dto.ForecastResponse;
import ru.polyrythms.weatherservice.dto.owm.OpenWeatherMapResponse;
import ru.polyrythms.weatherservice.exception.WeatherServiceException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final OpenWeatherMapClient weatherClient;
    private final ForecastAggregator aggregator;
    private final CityService cityService;

    @Cacheable(value = "weather", key = "#cityName")
    public ForecastResponse getForecast(String cityName) {
        log.info("Fetching forecast for city: {}", cityName);
        OpenWeatherMapResponse response = weatherClient.getForecast(cityName).block();
        if (response == null) {
            throw new WeatherServiceException("No data received from weather API");
        }
        var forecasts = aggregator.aggregate(response);
        return ForecastResponse.builder()
                .city(cityName)
                .forecasts(forecasts)
                .build();
    }

    public boolean isCityAllowed(Long cityId, List<Long> allowedCityIds) {
        return allowedCityIds != null && allowedCityIds.contains(cityId);
    }
}