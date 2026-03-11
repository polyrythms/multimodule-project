package ru.polyrythms.telegrambot.application.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CommandContext {
    Long chatId;
    Long userId;
    String command;
    String[] args;
    String fullText;
    String username;
    boolean isPrivateChat;
}