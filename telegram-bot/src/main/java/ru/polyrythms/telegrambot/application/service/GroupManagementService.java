package ru.polyrythms.telegrambot.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.polyrythms.telegrambot.application.port.input.GroupManagementUseCase;
import ru.polyrythms.telegrambot.application.port.output.AdminRepository;
import ru.polyrythms.telegrambot.application.port.output.TelegramGroupRepository;
import ru.polyrythms.telegrambot.domain.model.AdminUser;
import ru.polyrythms.telegrambot.domain.model.TelegramGroup;
import ru.polyrythms.telegrambot.domain.exception.UnauthorizedException;
import ru.polyrythms.telegrambot.domain.exception.DomainException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class GroupManagementService implements GroupManagementUseCase {

    private final TelegramGroupRepository groupRepository;
    private final AdminRepository adminRepository;

    @Override
    public TelegramGroup addGroup(Long chatId, String title, Long addedBy) {
        AdminUser admin = adminRepository.findByUserId(addedBy)
                .orElseThrow(() -> new UnauthorizedException("Пользователь не найден"));

        if (!admin.getRole().canManageGroups()) {
            throw new UnauthorizedException("Недостаточно прав для добавления групп");
        }

        if (groupRepository.existsByChatId(chatId)) {
            throw new DomainException("Группа с chatId " + chatId + " уже существует");
        }

        TelegramGroup group = TelegramGroup.builder()
                .chatId(chatId)
                .title(title)
                .isActive(true)
                .addedBy(addedBy)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TelegramGroup saved = groupRepository.save(group);
        log.info("Добавлена новая группа: '{}' (chatId: {}) пользователем {}", title, chatId, admin.getUsername());
        return saved;
    }

    @Override
    public void deactivateGroup(Long chatId, Long userId) {
        checkGroupManagementPermission(userId);

        TelegramGroup group = groupRepository.findByChatId(chatId)
                .orElseThrow(() -> new DomainException("Группа не найдена"));

        TelegramGroup deactivated = group.deactivate();
        groupRepository.save(deactivated);
        log.info("Группа деактивирована: '{}' (chatId: {})", group.getTitle(), chatId);
    }

    @Override
    public void activateGroup(Long chatId, Long userId) {
        checkGroupManagementPermission(userId);

        TelegramGroup group = groupRepository.findByChatId(chatId)
                .orElseThrow(() -> new DomainException("Группа не найдена"));

        TelegramGroup activated = group.activate();
        groupRepository.save(activated);
        log.info("Группа активирована: '{}' (chatId: {})", group.getTitle(), chatId);
    }

    @Override
    public List<TelegramGroup> getAllGroups() {
        List<TelegramGroup> groups = groupRepository.findAll();
        return enrichWithUsernames(groups);
    }

    @Override
    public List<TelegramGroup> getActiveGroups() {
        List<TelegramGroup> groups = groupRepository.findAllActive();
        return enrichWithUsernames(groups);
    }

    @Override
    public boolean isGroupAllowed(Long chatId) {
        return groupRepository.findByChatId(chatId)
                .map(TelegramGroup::getIsActive)
                .orElse(false);
    }

    @Override
    public long getActiveGroupsCount() {
        return groupRepository.countActive();
    }

    @Override
    public long getUserGroupsCount(Long userId) {
        return groupRepository.countByAddedBy(userId);
    }

    @Override
    public TelegramGroup getGroupByChatId(Long chatId) {
        TelegramGroup group = groupRepository.findByChatId(chatId).orElse(null);
        if (group != null) {
            return enrichWithUsername(group);
        }
        return null;
    }

    private void checkGroupManagementPermission(Long userId) {
        AdminUser admin = adminRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException("Пользователь не найден"));

        if (!admin.getRole().canManageGroups()) {
            throw new UnauthorizedException("Недостаточно прав для управления группами");
        }
    }

    private List<TelegramGroup> enrichWithUsernames(List<TelegramGroup> groups) {
        return groups.stream()
                .map(this::enrichWithUsername)
                .collect(Collectors.toList());
    }

    private TelegramGroup enrichWithUsername(TelegramGroup group) {
        return adminRepository.findByUserId(group.getAddedBy())
                .map(admin -> TelegramGroup.builder()
                        .id(group.getId())
                        .chatId(group.getChatId())
                        .title(group.getTitle())
                        .isActive(group.getIsActive())
                        .addedBy(group.getAddedBy())
                        .createdAt(group.getCreatedAt())
                        .updatedAt(group.getUpdatedAt())
                        .addedByUsername(admin.getUsername())
                        .build())
                .orElse(group);
    }
}