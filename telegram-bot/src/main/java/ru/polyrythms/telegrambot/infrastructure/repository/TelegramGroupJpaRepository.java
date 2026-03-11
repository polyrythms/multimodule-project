package ru.polyrythms.telegrambot.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.polyrythms.telegrambot.infrastructure.entity.TelegramGroupEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface TelegramGroupJpaRepository extends JpaRepository<TelegramGroupEntity, Long> {

    Optional<TelegramGroupEntity> findByChatId(Long chatId);

    List<TelegramGroupEntity> findByIsActiveTrue();

    List<TelegramGroupEntity> findByAddedBy(Long addedBy);

    boolean existsByChatId(Long chatId);

    long countByIsActiveTrue();

    @Query("SELECT COUNT(g) FROM TelegramGroupEntity g WHERE g.addedBy = :userId")
    long countByAddedBy(Long userId);
}