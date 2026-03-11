// infrastructure/adapter/mapper/AdminEntityMapper.java
package ru.polyrythms.telegrambot.infrastructure.adapter.mapper;

import org.springframework.stereotype.Component;
import ru.polyrythms.telegrambot.domain.model.AdminUser;
import ru.polyrythms.telegrambot.infrastructure.entity.AdminUserEntity;

@Component
public class AdminEntityMapper {

    public AdminUser toDomain(AdminUserEntity entity) {
        if (entity == null) return null;

        return AdminUser.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .username(entity.getUsername())
                .role(entity.getRole())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public AdminUserEntity toEntity(AdminUser domain) {
        if (domain == null) return null;

        AdminUserEntity entity = new AdminUserEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setUsername(domain.getUsername());
        entity.setRole(domain.getRole());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}