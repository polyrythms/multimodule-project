package ru.polyrythms.telegrambot.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.polyrythms.telegrambot.application.port.input.WeatherAdminUseCase;
import ru.polyrythms.telegrambot.application.port.output.AdminRepository;
import ru.polyrythms.telegrambot.application.port.output.CityRepository;
import ru.polyrythms.telegrambot.application.port.output.GroupCityRepository;
import ru.polyrythms.telegrambot.application.port.output.TelegramGroupRepository;
import ru.polyrythms.telegrambot.domain.exception.UnauthorizedException;
import ru.polyrythms.telegrambot.domain.exception.DomainException;
import ru.polyrythms.telegrambot.domain.model.City;
import ru.polyrythms.telegrambot.domain.model.TelegramGroup;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherAdminService implements WeatherAdminUseCase {
    private final CityRepository cityRepository;
    private final GroupCityRepository groupCityRepository;
    private final TelegramGroupRepository groupRepository;
    private final AdminRepository adminRepository;

    @Override
    public City addCity(String name) {
        if (cityRepository.findByName(name).isPresent()) {
            throw new DomainException("Город уже существует: " + name);
        }
        City city = City.builder().name(name).build();
        return cityRepository.save(city);
    }

    @Override
    public List<City> listCities() {
        return cityRepository.findAll();
    }

    @Override
    public void assignCityToGroup(Long groupChatId, Long cityId, Long adminId) {
        checkGroupManagementPermission(adminId);
        TelegramGroup group = groupRepository.findByChatId(groupChatId)
                .orElseThrow(() -> new DomainException("Группа не найдена"));
        if (!group.getIsActive()) {
            throw new DomainException("Группа неактивна");
        }
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new DomainException("Город не найден"));
        groupCityRepository.assignCityToGroup(groupChatId, cityId);
        log.info("City {} assigned to group {}", city.getName(), groupChatId);
    }

    @Override
    public void removeCityFromGroup(Long groupChatId, Long cityId, Long adminId) {
        checkGroupManagementPermission(adminId);
        groupCityRepository.removeCityFromGroup(groupChatId, cityId);
        log.info("City {} removed from group {}", cityId, groupChatId);
    }

    @Override
    public List<City> getCitiesForGroup(Long groupChatId) {
        return groupCityRepository.findByGroupChatId(groupChatId).stream()
                .map(gc -> cityRepository.findById(gc.getCityId()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private void checkGroupManagementPermission(Long adminId) {
        var admin = adminRepository.findByUserId(adminId)
                .orElseThrow(() -> new UnauthorizedException("Недостаточно прав"));
        if (!admin.getRole().canManageGroups()) {
            throw new UnauthorizedException("Недостаточно прав для управления группами");
        }
    }
}