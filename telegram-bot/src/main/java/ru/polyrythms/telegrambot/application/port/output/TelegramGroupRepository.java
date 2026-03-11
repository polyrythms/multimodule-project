package ru.polyrythms.telegrambot.application.port.output;

import ru.polyrythms.telegrambot.domain.model.TelegramGroup;

import java.util.List;
import java.util.Optional;

public interface TelegramGroupRepository {
    Optional<TelegramGroup> findByChatId(Long chatId);
    List<TelegramGroup> findAllActive();
    List<TelegramGroup> findAll();
    List<TelegramGroup> findByAddedBy(Long addedBy);
    boolean existsByChatId(Long chatId);
    TelegramGroup save(TelegramGroup group);
    void delete(TelegramGroup group);
    long countActive();
    long countByAddedBy(Long userId);
}