package ru.polyrythms.telegrambot.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.polyrythms.telegrambot.infrastructure.entity.GroupCityEntity;
import java.util.List;

public interface GroupCityJpaRepository extends JpaRepository<GroupCityEntity, Long> {
    List<GroupCityEntity> findByGroupChatId(Long groupChatId);
    void deleteByGroupChatIdAndCityId(Long groupChatId, Long cityId);
}