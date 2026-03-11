// application/dto/TelegramGroupDto.java
package ru.polyrythms.telegrambot.application.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TelegramGroupDto {
    Long chatId;
    String title;
    Boolean isActive;
    String addedByUsername;
    String createdAt;

    public static TelegramGroupDto fromDomain(ru.polyrythms.telegrambot.domain.model.TelegramGroup group) {
        return TelegramGroupDto.builder()
                .chatId(group.getChatId())
                .title(group.getTitle())
                .isActive(group.getIsActive())
                .addedByUsername(group.getAddedByUsername())
                .createdAt(group.getCreatedAt() != null ? group.getCreatedAt().toString() : null)
                .build();
    }
}