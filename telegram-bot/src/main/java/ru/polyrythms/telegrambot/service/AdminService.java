package ru.polyrythms.telegrambot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.polyrythms.telegrambot.entity.AdminUser;
import ru.polyrythms.telegrambot.entity.AdminRole;
import ru.polyrythms.telegrambot.repository.AdminUserRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminUserRepository adminRepository;

    public boolean isAdmin(Long userId) {
        return adminRepository.existsByUserId(userId);
    }

    public Optional<AdminUser> getAdmin(Long userId) {
        return adminRepository.findByUserId(userId);
    }

    public AdminRole getUserRole(Long userId) {
        return adminRepository.findByUserId(userId)
                .map(AdminUser::getRole)
                .orElse(null);
    }

    public boolean canManageAdmins(Long userId) {
        return adminRepository.findByUserId(userId)
                .map(admin -> admin.getRole().canManageAdmins())
                .orElse(false);
    }

    public boolean canManageGroups(Long userId) {
        return adminRepository.findByUserId(userId)
                .map(admin -> admin.getRole().canManageGroups())
                .orElse(false);
    }

    @Transactional
    public AdminUser addAdmin(Long ownerId, Long newAdminId, String username, AdminRole role) {
        // Проверяем права
        AdminUser owner = adminRepository.findByUserId(ownerId)
                .orElseThrow(() -> new SecurityException("Пользователь не найден"));

        if (!owner.getRole().canManageAdmins()) {
            throw new SecurityException("Недостаточно прав для добавления администраторов");
        }

        // Проверяем, не является ли пользователь уже админом
        if (adminRepository.existsByUserId(newAdminId)) {
            throw new IllegalArgumentException("Пользователь уже является администратором");
        }

        AdminUser newAdmin = new AdminUser();
        newAdmin.setUserId(newAdminId);
        newAdmin.setUsername(username);
        newAdmin.setRole(role);
        newAdmin.setCreatedBy(ownerId);

        AdminUser saved = adminRepository.save(newAdmin);
        log.info("Добавлен новый администратор: {} (ID: {}) с ролью {}",
                username, newAdminId, role);

        return saved;
    }

    @Transactional
    public void removeAdmin(Long ownerId, Long adminToRemoveId) {
        // Проверяем права
        AdminUser owner = adminRepository.findByUserId(ownerId)
                .orElseThrow(() -> new SecurityException("Пользователь не найден"));

        if (!owner.getRole().canManageAdmins()) {
            throw new SecurityException("Недостаточно прав для удаления администраторов");
        }

        AdminUser adminToRemove = adminRepository.findByUserId(adminToRemoveId)
                .orElseThrow(() -> new IllegalArgumentException("Администратор не найден"));

        // Нельзя удалить владельца
        if (adminToRemove.getRole() == AdminRole.OWNER) {
            throw new SecurityException("Нельзя удалить владельца системы");
        }

        adminRepository.delete(adminToRemove);
        log.info("Удален администратор: {} (ID: {})",
                adminToRemove.getUsername(), adminToRemoveId);
    }

    public List<AdminUser> getAllAdmins() {
        return adminRepository.findAll();
    }

    public List<AdminUser> getAdminsByRole(AdminRole role) {
        return adminRepository.findByRoleIn(List.of(role));
    }

    public long getAdminCount() {
        return adminRepository.count();
    }

    public long getAdminCountByRole(AdminRole role) {
        return adminRepository.countByRole(role);
    }
}