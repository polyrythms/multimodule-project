package ru.polyrythms.authservice.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.polyrythms.authservice.dto.GrantRequest;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final com.nimbusds.jose.jwk.RSAKey rsaKey;

    @Value("${jwt.ttl-seconds:3600}")
    private long ttlSeconds;

    public String generateToken(GrantRequest request) throws JOSEException {
        JWSSigner signer = new RSASSASigner(rsaKey.toPrivateKey());

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .claim("telegram_id", request.getTelegramId())
                .claim("chat_id", request.getChatId())
                .claim("city_ids", request.getCityIds())
                .claim("role", request.getRole())
                .issuer("auth-service")
                .audience("weather-api")
                .jwtID(UUID.randomUUID().toString())
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(ttlSeconds)))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                claimsSet
        );
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}