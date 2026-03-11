package ru.polyrythms.telegrambot.infrastructure.adapter.mapper;

import org.springframework.stereotype.Component;
import ru.polyrythms.telegrambot.domain.model.TelegramGroup;
import ru.polyrythms.telegrambot.infrastructure.entity.TelegramGroupEntity;

@Component
public class TelegramGroupEntityMapper {

    public TelegramGroup toDomain(TelegramGroupEntity entity) {
        if (entity == null) return null;

        return TelegramGroup.builder()
                .id(entity.getId())
                .chatId(entity.getChatId())
                .title(entity.getTitle())
                .isActive(entity.getIsActive())
                .addedBy(entity.getAddedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .addedByUsername(entity.getAddedByUsername())
                .build();
    }

    public TelegramGroupEntity toEntity(TelegramGroup domain) {
        if (domain == null) return null;

        TelegramGroupEntity entity = new TelegramGroupEntity();
        entity.setId(domain.getId());
        entity.setChatId(domain.getChatId());
        entity.setTitle(domain.getTitle());
        entity.setIsActive(domain.getIsActive());
        entity.setAddedBy(domain.getAddedBy());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setAddedByUsername(domain.getAddedByUsername());
        return entity;
    }
}