package ru.polyrythms.weatherservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.polyrythms.weatherservice.dto.owm.OpenWeatherMapResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenWeatherMapClient {

    private final WebClient openWeatherMapWebClient;

    @Value("${openweathermap.api.key}")
    private String apiKey;

    public Mono<OpenWeatherMapResponse> getForecast(String cityName) {
        return openWeatherMapWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/forecast")
                        .queryParam("q", cityName)
                        .queryParam("appid", apiKey)
                        .queryParam("units", "metric")
                        .queryParam("lang", "ru")
                        .build())
                .retrieve()
                .bodyToMono(OpenWeatherMapResponse.class)
                .doOnError(e -> log.error("Failed to fetch weather for city: {}", cityName, e));
    }
}