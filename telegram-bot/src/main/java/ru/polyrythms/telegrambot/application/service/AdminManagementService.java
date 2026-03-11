package ru.polyrythms.telegrambot.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.polyrythms.telegrambot.application.port.input.AdminManagementUseCase;
import ru.polyrythms.telegrambot.application.port.output.AdminRepository;
import ru.polyrythms.telegrambot.domain.model.AdminUser;
import ru.polyrythms.telegrambot.domain.model.AdminRole;
import ru.polyrythms.telegrambot.domain.exception.UnauthorizedException;
import ru.polyrythms.telegrambot.domain.exception.DomainException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class AdminManagementService implements AdminManagementUseCase {

    private final AdminRepository adminRepository;

    @Override
    public AdminUser addAdmin(Long ownerId, Long newAdminId, String username, AdminRole role) {
        AdminUser owner = adminRepository.findByUserId(ownerId)
                .orElseThrow(() -> new UnauthorizedException("Пользователь не найден"));

        if (!owner.isOwner()) {
            throw new UnauthorizedException("Недостаточно прав для добавления администраторов");
        }

        if (adminRepository.existsByUserId(newAdminId)) {
            throw new DomainException("Пользователь уже является администратором");
        }

        AdminUser newAdmin = AdminUser.builder()
                .userId(newAdminId)
                .username(username)
                .role(role)
                .createdBy(ownerId)
                .createdAt(LocalDateTime.now())
                .build();

        AdminUser saved = adminRepository.save(newAdmin);
        log.info("Добавлен новый администратор: {} (ID: {}) с ролью {}", username, newAdminId, role);
        return saved;
    }

    @Override
    public void removeAdmin(Long ownerId, Long adminToRemoveId) {
        AdminUser owner = adminRepository.findByUserId(ownerId)
                .orElseThrow(() -> new UnauthorizedException("Пользователь не найден"));

        if (!owner.isOwner()) {
            throw new UnauthorizedException("Недостаточно прав для удаления администраторов");
        }

        AdminUser adminToRemove = adminRepository.findByUserId(adminToRemoveId)
                .orElseThrow(() -> new DomainException("Администратор не найден"));

        if (adminToRemove.isOwner()) {
            throw new UnauthorizedException("Нельзя удалить владельца системы");
        }

        adminRepository.delete(adminToRemove);
        log.info("Удален администратор: {} (ID: {})", adminToRemove.getUsername(), adminToRemoveId);
    }

    @Override
    public List<AdminUser> getAllAdmins() {
        return adminRepository.findAll();
    }

    @Override
    public AdminUser getAdmin(Long userId) {
        return adminRepository.findByUserId(userId).orElse(null);
    }

    @Override
    public boolean isAdmin(Long userId) {
        return adminRepository.existsByUserId(userId);
    }

    @Override
    public AdminRole getUserRole(Long userId) {
        return adminRepository.findByUserId(userId)
                .map(AdminUser::getRole)
                .orElse(null);
    }

    @Override
    public long getAdminCount() {
        return adminRepository.count();
    }
}