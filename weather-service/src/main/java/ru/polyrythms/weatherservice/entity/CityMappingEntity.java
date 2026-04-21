package ru.polyrythms.weatherservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "city_mapping")
@Getter
@Setter
@NoArgsConstructor
public class CityMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_id", nullable = false, unique = true)
    private Long cityId;

    @Column(name = "city_name", nullable = false)
    private String cityName;

    @Column(name = "openweathermap_id")
    private Long openWeatherMapId;
}