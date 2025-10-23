package ru.polyrythms.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.polyrythms.telegrambot.entity.AdminUser;
import ru.polyrythms.telegrambot.entity.TelegramGroup;
import ru.polyrythms.telegrambot.repository.TelegramGroupRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramGroupService {

    private final TelegramGroupRepository groupRepository;
    private final AdminService adminService;

    public List<TelegramGroup> getActiveGroups() {
        return groupRepository.findByIsActiveTrue();
    }

    public List<TelegramGroup> getAllGroups() {
        List<TelegramGroup> groups = groupRepository.findAll();

        // Заполняем username для добавленных_by
        groups.forEach(group -> adminService.getAdmin(group.getAddedBy())
                .ifPresent(admin -> group.setAddedByUsername(admin.getUsername())));

        return groups;
    }

    public Optional<TelegramGroup> getGroupByChatId(Long chatId) {
        return groupRepository.findByChatId(chatId);
    }

    public boolean isGroupAllowed(Long chatId) {
        return groupRepository.findByChatId(chatId)
                .map(TelegramGroup::getIsActive)
                .orElse(false);
    }

    @Transactional
    public TelegramGroup addGroup(Long chatId, String title, Long addedBy) {
        // Проверяем права
        if (!adminService.canManageGroups(addedBy)) {
            throw new SecurityException("Недостаточно прав для добавления групп");
        }

        if (groupRepository.existsByChatId(chatId)) {
            throw new IllegalArgumentException("Группа с chatId " + chatId + " уже существует");
        }

        TelegramGroup group = new TelegramGroup();
        group.setChatId(chatId);
        group.setTitle(title);
        group.setIsActive(true);
        group.setAddedBy(addedBy);

        TelegramGroup saved = groupRepository.save(group);

        // Получаем username для логирования
        String username = adminService.getAdmin(addedBy)
                .map(AdminUser::getUsername)
                .orElse("Unknown");

        log.info("Добавлена новая группа: '{}' (chatId: {}) пользователем {}",
                title, chatId, username);

        return saved;
    }

    @Transactional
    public void deactivateGroup(Long chatId, Long userId) {
        // Проверяем права
        if (!adminService.canManageGroups(userId)) {
            throw new SecurityException("Недостаточно прав для управления группами");
        }

        TelegramGroup group = groupRepository.findByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));

        group.setIsActive(false);
        groupRepository.save(group);

        log.info("Группа деактивирована: '{}' (chatId: {})", group.getTitle(), chatId);
    }

    @Transactional
    public void activateGroup(Long chatId, Long userId) {
        // Проверяем права
        if (!adminService.canManageGroups(userId)) {
            throw new SecurityException("Недостаточно прав для управления группами");
        }

        TelegramGroup group = groupRepository.findByChatId(chatId)
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));

        group.setIsActive(true);
        groupRepository.save(group);

        log.info("Группа активирована: '{}' (chatId: {})", group.getTitle(), chatId);
    }

    public long getActiveGroupsCount() {
        return groupRepository.countByIsActiveTrue();
    }

    public long getUserGroupsCount(Long userId) {
        return groupRepository.countByAddedBy(userId);
    }
}