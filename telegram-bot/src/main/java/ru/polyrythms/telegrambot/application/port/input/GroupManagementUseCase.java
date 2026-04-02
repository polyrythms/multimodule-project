package ru.polyrythms.telegrambot.application.port.input;

import ru.polyrythms.telegrambot.domain.model.TelegramGroup;

import java.util.List;

public interface GroupManagementUseCase {
    TelegramGroup addGroup(Long chatId, String title, Long addedBy);
    void deactivateGroup(Long chatId, Long userId);
    void activateGroup(Long chatId, Long userId);
    List<TelegramGroup> getAllGroups();
    List<TelegramGroup> getActiveGroups();
    boolean isGroupAllowed(Long chatId);
    long getActiveGroupsCount();
    long getUserGroupsCount(Long userId);
    TelegramGroup getGroupByChatId(Long chatId);
}