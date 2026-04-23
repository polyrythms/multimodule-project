package ru.polyrythms.weatherservice.service;

import org.springframework.stereotype.Component;
import ru.polyrythms.weatherservice.dto.DailyForecast;
import ru.polyrythms.weatherservice.dto.owm.OpenWeatherMapResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ForecastAggregator {

    public List<DailyForecast> aggregate(OpenWeatherMapResponse response) {
        if (response == null || response.getList() == null || response.getList().isEmpty()) {
            return List.of();
        }

        Map<LocalDate, List<OpenWeatherMapResponse.ForecastItem>> grouped = response.getList().stream()
                .collect(Collectors.groupingBy(item -> Instant.ofEpochSecond(item.getDt())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()));

        List<DailyForecast> forecasts = new ArrayList<>();
        for (Map.Entry<LocalDate, List<OpenWeatherMapResponse.ForecastItem>> entry : grouped.entrySet()) {
            LocalDate date = entry.getKey();
            List<OpenWeatherMapResponse.ForecastItem> items = entry.getValue();

            double tempMax = items.stream().mapToDouble(i -> i.getMain().getTemp_max()).max().orElse(0);
            double tempMin = items.stream().mapToDouble(i -> i.getMain().getTemp_min()).min().orElse(0);
            double windSpeedMax = items.stream().mapToDouble(i -> i.getWind().getSpeed()).max().orElse(0);
            double windGustMax = items.stream().mapToDouble(i -> i.getWind().getGust()).max().orElse(0);
            int cloudiness = items.stream().mapToInt(i -> i.getClouds().getAll()).max().orElse(0);
            double rainMm = items.stream().mapToDouble(i -> i.getRain() != null ? i.getRain().getThreeHour() : 0).sum();
            double snowWaterEq = items.stream().mapToDouble(i -> i.getSnow() != null ? i.getSnow().getThreeHour() : 0).sum();
            double snowCm = snowWaterEq / 10.0; // 1 мм вод.экв. = 0.1 см снега

            String windDirection = getWindDirection(averageDegrees(items));

            forecasts.add(DailyForecast.builder()
                    .date(date)
                    .tempMax(tempMax)
                    .tempMin(tempMin)
                    .windSpeedMax(windSpeedMax)
                    .windGustMax(windGustMax > 0 ? windGustMax : null)
                    .windDirection(windDirection)
                    .cloudiness(cloudiness)
                    .rainMm(rainMm > 0 ? rainMm : null)
                    .snowCm(snowCm > 0 ? snowCm : null)
                    .build());
        }

        // Сортируем по дате и берём первые 3 дня (можно все)
        forecasts.sort(Comparator.comparing(DailyForecast::getDate));
        return forecasts.stream().limit(3).collect(Collectors.toList());
    }

    private double averageDegrees(List<OpenWeatherMapResponse.ForecastItem> items) {
        // Круговая статистика: среднее азимута
        double sumSin = 0.0;
        double sumCos = 0.0;
        for (var item : items) {
            double deg = Math.toRadians(item.getWind().getDeg());
            sumSin += Math.sin(deg);
            sumCos += Math.cos(deg);
        }
        double avgRad = Math.atan2(sumSin, sumCos);
        double avgDeg = Math.toDegrees(avgRad);
        if (avgDeg < 0) avgDeg += 360;
        return avgDeg;
    }

    private String getWindDirection(double deg) {
        if (deg < 0 || deg > 360) return "?";
        String[] directions = {"С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ"};
        int index = (int) Math.round(deg / 45.0) % 8;
        return directions[index];
    }
}