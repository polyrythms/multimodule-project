package ru.polyrythms.telegrambot.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class AdminUser {
    Long id;
    Long userId;
    String username;
    AdminRole role;
    Long createdBy;
    LocalDateTime createdAt;

    public boolean isOwner() {
        return role == AdminRole.OWNER;
    }

    public boolean canManage(AdminUser target) {
        return this.isOwner() || (this.role.canManageAdmins() && target.role.getLevel() < this.role.getLevel());
    }
}