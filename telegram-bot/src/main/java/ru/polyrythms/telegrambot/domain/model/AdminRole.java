package ru.polyrythms.telegrambot.domain.model;

public enum AdminRole {
    OWNER(100),
    ADMIN(50),
    MODERATOR(10);

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
        return true;
    }

    public int getLevel() {
        return level;
    }
}