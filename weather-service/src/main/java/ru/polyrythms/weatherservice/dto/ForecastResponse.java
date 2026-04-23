package ru.polyrythms.weatherservice.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ForecastResponse {
    private String city;
    private List<DailyForecast> forecasts;
}