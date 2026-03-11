package ru.polyrythms.telegrambot.application.port.output;

import ru.polyrythms.telegrambot.domain.model.AdminUser;
import ru.polyrythms.telegrambot.domain.model.AdminRole;

import java.util.List;
import java.util.Optional;

public interface AdminRepository {
    Optional<AdminUser> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    AdminUser save(AdminUser adminUser);
    void delete(AdminUser adminUser);
    List<AdminUser> findAll();
    List<AdminUser> findByRole(AdminRole role);
    long count();
    long countByRole(AdminRole role);
}