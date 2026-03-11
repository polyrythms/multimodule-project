package ru.polyrythms.telegrambot.application.port.input;

import ru.polyrythms.telegrambot.domain.model.AdminUser;
import ru.polyrythms.telegrambot.domain.model.AdminRole;

import java.util.List;

public interface AdminManagementUseCase {
    AdminUser addAdmin(Long ownerId, Long newAdminId, String username, AdminRole role);
    void removeAdmin(Long ownerId, Long adminToRemoveId);
    List<AdminUser> getAllAdmins();
    AdminUser getAdmin(Long userId);
    boolean isAdmin(Long userId);
    AdminRole getUserRole(Long userId);
    long getAdminCount();
}