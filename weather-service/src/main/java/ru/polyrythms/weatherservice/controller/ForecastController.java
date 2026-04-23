package ru.polyrythms.weatherservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.polyrythms.weatherservice.dto.CityDto;
import ru.polyrythms.weatherservice.dto.ForecastResponse;
import ru.polyrythms.weatherservice.exception.WeatherServiceException;
import ru.polyrythms.weatherservice.service.CityService;
import ru.polyrythms.weatherservice.service.WeatherService;

import java.util.List;

@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class ForecastController {

    private final WeatherService weatherService;
    private final CityService cityService;

    @GetMapping("/forecast")
    public ForecastResponse getForecast(@RequestParam String city,
                                        @AuthenticationPrincipal Jwt jwt) {
        List<Long> allowedCityIds = jwt.getClaim("city_ids");
        if (allowedCityIds == null || allowedCityIds.isEmpty()) {
            throw new WeatherServiceException("No cities assigned to this user");
        }

        // Находим city_id по названию (через маппинг)
        Long cityId = cityService.getCitiesByIds(allowedCityIds).stream()
                .filter(c -> c.getName().equalsIgnoreCase(city))
                .map(CityDto::getId)
                .findFirst()
                .orElseThrow(() -> new WeatherServiceException("City not allowed or not found: " + city));

        if (!weatherService.isCityAllowed(cityId, allowedCityIds)) {
            throw new WeatherServiceException("Access denied to city: " + city);
        }

        return weatherService.getForecast(city);
    }
}