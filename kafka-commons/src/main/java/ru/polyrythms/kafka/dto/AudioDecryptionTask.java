package ru.polyrythms.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AudioDecryptionTask implements Serializable {
    private String taskId;
    private String audioId;
    private Long chatId;
    private String audioUrl;
    private Long createdAt;
    private AudioType audioType;

    public static AudioDecryptionTask createVoiceTask(String audioId, Long chatId, String audioUrl) {
        return AudioDecryptionTask.builder()
                .taskId(UUID.randomUUID().toString())
                .audioId(audioId)
                .chatId(chatId)
                .audioUrl(audioUrl)
                .createdAt(System.currentTimeMillis())
                .audioType(AudioType.VOICE_MESSAGE)
                .build();
    }

    public Instant getCreatedAtAsInstant() {
        return Instant.ofEpochMilli(createdAt);
    }

    public enum AudioType {
        AUDIO_FILE, VOICE_MESSAGE, VIDEO_NOTE
    }
}