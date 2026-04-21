package ru.polyrythms.weatherservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.polyrythms.weatherservice.dto.CityDto;
import ru.polyrythms.weatherservice.service.CityService;

import java.util.List;

@RestController
@RequestMapping("/weather")
@RequiredArgsConstructor
public class CitiesController {

    private final CityService cityService;

    @GetMapping("/cities")
    public List<CityDto> getCities(@AuthenticationPrincipal Jwt jwt) {
        List<Long> cityIds = jwt.getClaim("city_ids");
        if (cityIds == null) cityIds = List.of();
        return cityService.getCitiesByIds(cityIds);
    }
}