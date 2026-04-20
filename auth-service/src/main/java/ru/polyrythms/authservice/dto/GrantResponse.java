package ru.polyrythms.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GrantResponse {
    private String accessToken;
    private long expiresIn;
}