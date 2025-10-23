package ru.polyrythms.telegrambot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.polyrythms.telegrambot.entity.TelegramGroup;

import java.util.List;
import java.util.Optional;

@Repository
public interface TelegramGroupRepository extends JpaRepository<TelegramGroup, Long> {

    Optional<TelegramGroup> findByChatId(Long chatId);

    List<TelegramGroup> findByIsActiveTrue();

    List<TelegramGroup> findByAddedBy(Long addedBy);

    boolean existsByChatId(Long chatId);

    long countByIsActiveTrue();

    @Query("SELECT COUNT(g) FROM TelegramGroup g WHERE g.addedBy = :userId")
    long countByAddedBy(Long userId);
}