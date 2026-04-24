package ru.polyrythms.telegrambot.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InitDataRequest {
    @NotBlank
    private String initData;
}