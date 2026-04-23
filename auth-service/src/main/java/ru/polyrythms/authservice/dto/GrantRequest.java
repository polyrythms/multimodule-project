package ru.polyrythms.authservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class GrantRequest {
    private Long telegramId;
    private Long chatId;
    private List<Long> cityIds;
    private String role; // "MEMBER"
}