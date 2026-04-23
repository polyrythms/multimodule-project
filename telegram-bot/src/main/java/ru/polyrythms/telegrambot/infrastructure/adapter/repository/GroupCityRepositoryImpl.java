package ru.polyrythms.telegrambot.infrastructure.adapter.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.polyrythms.telegrambot.application.port.output.GroupCityRepository;
import ru.polyrythms.telegrambot.domain.model.GroupCity;
import ru.polyrythms.telegrambot.infrastructure.entity.GroupCityEntity;
import ru.polyrythms.telegrambot.infrastructure.repository.GroupCityJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class GroupCityRepositoryImpl implements GroupCityRepository {
    private final GroupCityJpaRepository jpaRepository;

    @Override
    public List<GroupCity> findByGroupChatId(Long groupChatId) {
        return jpaRepository.findByGroupChatId(groupChatId).stream()
                .map(e -> GroupCity.builder()
                        .groupChatId(e.getGroupChatId())
                        .cityId(e.getCityId())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public void assignCityToGroup(Long groupChatId, Long cityId) {
        GroupCityEntity entity = new GroupCityEntity();
        entity.setGroupChatId(groupChatId);
        entity.setCityId(cityId);
        entity.setAssignedAt(LocalDateTime.now());
        jpaRepository.save(entity);
    }

    @Override
    public void removeCityFromGroup(Long groupChatId, Long cityId) {
        jpaRepository.deleteByGroupChatIdAndCityId(groupChatId, cityId);
    }
}