package ru.polyrythms.telegrambot.entity;

import lombok.Getter;

@Getter
public enum AdminRole {
    OWNER(100),    // Владелец - полные права
    ADMIN(50),     // Администратор - управление группами
    MODERATOR(10); // Модератор - только просмотр

    private final int level;

    AdminRole(int level) {
        this.level = level;
    }

    public boolean canManageAdmins() {
        return this == OWNER;
    }

    public boolean canManageGroups() {
        return this.level >= ADMIN.level;
    }

    public boolean canViewStats() {
        return true; // Все роли могут просматривать статистику
    }
}