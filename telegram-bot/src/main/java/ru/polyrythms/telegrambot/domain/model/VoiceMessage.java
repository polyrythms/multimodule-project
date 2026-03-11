package ru.polyrythms.telegrambot.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VoiceMessage {
    String fileId;
    String fileUniqueId;
    Long chatId;
    Long userId;
    Integer duration;
    Boolean isGroupChat;
}