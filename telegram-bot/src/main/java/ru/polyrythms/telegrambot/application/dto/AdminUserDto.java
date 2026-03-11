package ru.polyrythms.telegrambot.application.dto;

import lombok.Builder;
import lombok.Value;
import ru.polyrythms.telegrambot.domain.model.AdminRole;

@Value
@Builder
public class AdminUserDto {
    Long userId;
    String username;
    AdminRole role;
    String createdAt;

    public static AdminUserDto fromDomain(ru.polyrythms.telegrambot.domain.model.AdminUser admin) {
        return AdminUserDto.builder()
                .userId(admin.getUserId())
                .username(admin.getUsername())
                .role(admin.getRole())
                .createdAt(admin.getCreatedAt() != null ? admin.getCreatedAt().toString() : null)
                .build();
    }
}