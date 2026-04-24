package ru.polyrythms.telegrambot.infrastructure.adapter.input.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.polyrythms.telegrambot.application.dto.InitDataRequest;
import ru.polyrythms.telegrambot.application.dto.TokenResponse;
import ru.polyrythms.telegrambot.application.port.input.WeatherUserUseCase;
import ru.polyrythms.telegrambot.domain.exception.UnauthorizedException;
import ru.polyrythms.telegrambot.infrastructure.metrics.BotMetrics;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final WeatherUserUseCase weatherUserUseCase;
    private final BotMetrics botMetrics;

    @PostMapping("/init")
    public ResponseEntity<TokenResponse> authByInitData(@Valid @RequestBody InitDataRequest request) {
        log.info("Received /auth/init request");
        botMetrics.recordAuthInitAttempt();

        try {
            String token = weatherUserUseCase.authenticateWithInitData(request.getInitData());
            botMetrics.recordAuthInitSuccess();
            return ResponseEntity.ok(new TokenResponse(token));
        } catch (SecurityException | UnauthorizedException e) {
            log.warn("Auth failed: {}", e.getMessage());
            botMetrics.recordAuthInitFailure();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new TokenResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during auth", e);
            botMetrics.recordAuthInitFailure();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new TokenResponse("Внутренняя ошибка сервера. Попробуйте позже."));
        }
    }
}