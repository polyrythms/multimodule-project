package ru.polyrythms.telegrambot.infrastructure.adapter.input.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.polyrythms.telegrambot.application.port.input.WeatherUserUseCase;

@Slf4j
@RestController
@RequestMapping("/exchange")
@RequiredArgsConstructor
@Deprecated
public class ExchangeController {
    private final WeatherUserUseCase weatherUserUseCase;

    @PostMapping
    public ResponseEntity<ExchangeResponse> exchange(@Valid @RequestBody ExchangeRequest request) {
        String token = weatherUserUseCase.exchangeCode(request.code, request.initData);
        return ResponseEntity.ok(new ExchangeResponse(token));
    }

    public record ExchangeRequest(@NotBlank String code, @NotBlank String initData) {}
    public record ExchangeResponse(String accessToken) {}
}