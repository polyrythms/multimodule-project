package ru.polyrythms.authservice.controller;

import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.polyrythms.authservice.dto.GrantRequest;
import ru.polyrythms.authservice.dto.GrantResponse;
import ru.polyrythms.authservice.service.JwtService;

@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalGrantController {

    private final JwtService jwtService;
    private final static long TOKEN_EXPIRES_IN_SECONDS = 3600;

    @PostMapping("/grant")
    public ResponseEntity<GrantResponse> grant(@RequestBody GrantRequest request) {
        log.info("Internal grant request for telegramId: {}, chatId: {}", request.getTelegramId(), request.getChatId());
        try {
            String token = jwtService.generateToken(request);
            return ResponseEntity.ok(new GrantResponse(token, TOKEN_EXPIRES_IN_SECONDS));
        } catch (JOSEException e) {
            log.error("JWT generation failed", e);
            return ResponseEntity.status(500).build();
        }
    }
}