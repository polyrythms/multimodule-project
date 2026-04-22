package ru.polyrythms.telegrambot.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GroupCity {
    Long groupChatId;
    Long cityId;
}