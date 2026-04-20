package ru.polyrythms.authservice.config;

import com.nimbusds.jose.jwk.RSAKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

@Slf4j
@Configuration
public class RsaKeyConfig {

    @Value("${jwt.private-key:}")
    private String privateKeyBase64;

    @Value("${jwt.public-key:}")
    private String publicKeyBase64;

    @Getter
    private RSAPublicKey publicKey;

    @Getter
    private RSAPrivateKey privateKey;

    @Bean
    public RSAKey rsaKey() {
        if (!privateKeyBase64.isEmpty() && !publicKeyBase64.isEmpty()) {
            // Загружаем ключи из переменных окружения
            byte[] privateBytes = Base64.getDecoder().decode(privateKeyBase64);
            byte[] publicBytes = Base64.getDecoder().decode(publicKeyBase64);
            try {
                java.security.spec.PKCS8EncodedKeySpec privateSpec = new java.security.spec.PKCS8EncodedKeySpec(privateBytes);
                java.security.spec.X509EncodedKeySpec publicSpec = new java.security.spec.X509EncodedKeySpec(publicBytes);
                java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
                this.privateKey = (RSAPrivateKey) kf.generatePrivate(privateSpec);
                this.publicKey = (RSAPublicKey) kf.generatePublic(publicSpec);
                log.info("Loaded RSA keys from environment variables");
            } catch (Exception e) {
                throw new RuntimeException("Failed to load RSA keys from env", e);
            }
        } else {
            // Генерируем случайные ключи
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair keyPair = generator.generateKeyPair();
                this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
                this.publicKey = (RSAPublicKey) keyPair.getPublic();
                log.info("Generated new RSA key pair (2048 bit)");
                // Для отладки можно вывести public key base64 (не обязательно)
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("RSA algorithm not available", e);
            }
        }

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("auth-key-1")
                .build();
    }
}