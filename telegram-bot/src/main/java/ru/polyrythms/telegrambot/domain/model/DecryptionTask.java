package ru.polyrythms.telegrambot.domain.model;

import lombok.Builder;
import lombok.Value;
import java.time.LocalDateTime;

@Value
@Builder
public class DecryptionTask {
    String taskId;
    String audioId;
    Long chatId;
    String audioUrl;
    LocalDateTime createdAt;
    TaskStatus status;

    public enum TaskStatus {
        CREATED, PROCESSING, COMPLETED, FAILED
    }
}