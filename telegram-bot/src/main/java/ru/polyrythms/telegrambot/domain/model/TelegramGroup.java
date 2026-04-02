package ru.polyrythms.telegrambot.domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class TelegramGroup {
    Long id;
    Long chatId;
    String title;
    Boolean isActive;
    Long addedBy;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    String addedByUsername;

    public TelegramGroup activate() {
        return TelegramGroup.builder()
                .id(this.id)
                .chatId(this.chatId)
                .title(this.title)
                .isActive(true)
                .addedBy(this.addedBy)
                .createdAt(this.createdAt)
                .updatedAt(LocalDateTime.now())
                .addedByUsername(this.addedByUsername)
                .build();
    }

    public TelegramGroup deactivate() {
        return TelegramGroup.builder()
                .id(this.id)
                .chatId(this.chatId)
                .title(this.title)
                .isActive(false)
                .addedBy(this.addedBy)
                .createdAt(this.createdAt)
                .updatedAt(LocalDateTime.now())
                .addedByUsername(this.addedByUsername)
                .build();
    }
}