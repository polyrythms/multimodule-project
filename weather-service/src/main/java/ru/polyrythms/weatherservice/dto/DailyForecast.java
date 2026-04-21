package ru.polyrythms.weatherservice.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class DailyForecast {
    private LocalDate date;
    private Double tempMax;
    private Double tempMin;
    private Double windSpeedMax;
    private Double windGustMax;
    private String windDirection;
    private Integer cloudiness;
    private Double rainMm;
    private Double snowCm;
}