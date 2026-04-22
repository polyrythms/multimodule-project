package ru.polyrythms.telegrambot.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import ru.polyrythms.telegrambot.application.port.input.WeatherAdminUseCase;
import ru.polyrythms.telegrambot.application.port.input.WeatherUserUseCase;
import ru.polyrythms.telegrambot.domain.model.City;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherUserService implements WeatherUserUseCase {

    private final WeatherAdminUseCase weatherAdmin;
    private final WebClient.Builder webClientBuilder;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${internal.auth.key}")
    private String internalAuthKey;

    @Value("${auth.service.url:http://auth-service:8080}")
    private String authServiceUrl;

    // Хранилище кодов: code -> CodeData
    private final Map<String, CodeData> codeStore = new ConcurrentHashMap<>();
    private static final long CODE_TTL_MS = 5 * 60 * 1000; // 5 минут
    private static final long INIT_DATA_MAX_AGE_SECONDS = 24 * 60 * 60; // 24 часа

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public String generateWeatherCode(Long userId, Long chatId) {
        List<City> cities = weatherAdmin.getCitiesForGroup(chatId);
        if (cities.isEmpty()) {
            throw new IllegalStateException("Нет доступных городов для этой группы");
        }
        List<Long> cityIds = cities.stream().map(City::getId).toList();
        String code = UUID.randomUUID().toString();
        codeStore.put(code, new CodeData(userId, chatId, cityIds, System.currentTimeMillis() + CODE_TTL_MS));
        log.debug("Generated code {} for userId {} chatId {}", code, userId, chatId);
        return code;
    }

    @Override
    public String exchangeCode(String code, String initData) {
//        // 1. Проверка подписи initData
//        if (!verifyInitData(initData)) {
//            log.warn("Invalid initData signature");
//            throw new SecurityException("Неверная подпись данных");
//        }
//
//        // 2. Парсинг параметров и извлечение данных
//        Map<String, String> params = parseInitDataParams(initData);
//        Long userIdFromInit = extractUserId(params);
//        if (userIdFromInit == null) {
//            throw new SecurityException("Не удалось извлечь user.id из initData");
//        }
//
//        // 3. Проверка срока действия auth_date
//        if (!isAuthDateValid(params)) {
//            throw new SecurityException("Данные устарели");
//        }

        // 4. Проверка кода
        CodeData data = codeStore.remove(code);
        if (data == null || data.expiresAt < System.currentTimeMillis()) {
            throw new IllegalArgumentException("Неверный или просроченный код");
        }
//
//        // 5. Сравнение userId
//        if (!data.userId.equals(userIdFromInit)) {
//            log.warn("User mismatch: code userId={}, initData userId={}", data.userId, userIdFromInit);
//            throw new SecurityException("Пользователь не соответствует коду");
//        }

        // 6. Вызов auth-service
        return callAuthService(data.userId, data.chatId, data.cityIds);
    }

    // ==================== Верификация initData (ручная) ====================

    /**
     * Проверяет подпись initData согласно спецификации Telegram.
     * Алгоритм:
     * 1. Удалить параметр hash.
     * 2. Оставшиеся пары key=value отсортировать по ключу и объединить через \n.
     * 3. Вычислить HMAC-SHA256 от полученной строки, используя секретный ключ:
     *    secret = HMAC-SHA256("WebAppData", botToken)
     * 4. Сравнить полученный хеш с переданным (в шестнадцатеричном виде).
     */
    private boolean verifyInitData(String initData) {
        if (initData == null || initData.isBlank()) return false;

        Map<String, String> params = parseInitDataParams(initData);
        String hash = params.remove("hash");
        if (hash == null || hash.isEmpty()) return false;

        // Формируем строку для проверки: отсортированные key=value через \n
        String checkString = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        // Генерация секретного ключа
        String secretKey = hmacSha256("WebAppData", botToken);
        String expectedHash = hmacSha256(checkString, secretKey);

        return expectedHash.equals(hash);
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(result);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC-SHA256 error", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private Map<String, String> parseInitDataParams(String initData) {
        return Arrays.stream(initData.split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(
                        arr -> arr[0],
                        arr -> arr.length > 1 ? urlDecode(arr[1]) : "",
                        (a, b) -> a
                ));
    }

    private String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    // ==================== Извлечение данных из initData ====================

    private Long extractUserId(Map<String, String> params) {
        String userJson = params.get("user");
        if (userJson == null) return null;
        try {
            JsonNode node = objectMapper.readTree(userJson);
            return node.get("id").asLong();
        } catch (Exception e) {
            log.warn("Failed to parse user JSON: {}", userJson, e);
            return null;
        }
    }

    private boolean isAuthDateValid(Map<String, String> params) {
        String authDateStr = params.get("auth_date");
        if (authDateStr == null) return false;
        try {
            long authDateSeconds = Long.parseLong(authDateStr);
            long nowSeconds = System.currentTimeMillis() / 1000;
            return (nowSeconds - authDateSeconds) <= INIT_DATA_MAX_AGE_SECONDS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ==================== Вызов auth-service ====================

    private String callAuthService(Long userId, Long chatId, List<Long> cityIds) {
        try {
            WebClient client = webClientBuilder.baseUrl(authServiceUrl).build();
            Map<String, Object> request = Map.of(
                    "telegramId", userId,
                    "chatId", chatId,
                    "cityIds", cityIds,
                    "role", "MEMBER"
            );
            var response = client.post()
                    .uri("/internal/grant")
                    .header("X-Internal-Auth", internalAuthKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (response == null || !response.containsKey("accessToken")) {
                throw new RuntimeException("Неверный ответ от auth-service");
            }
            return (String) response.get("accessToken");
        } catch (Exception e) {
            log.error("Failed to call auth-service", e);
            throw new RuntimeException("Ошибка авторизации");
        }
    }

    // ==================== Очистка просроченных кодов ====================

    @Scheduled(fixedRate = 60000) // каждую минуту
    public void cleanExpiredCodes() {
        long now = System.currentTimeMillis();
        int removed = 0;
        for (var entry : codeStore.entrySet()) {
            if (entry.getValue().expiresAt < now) {
                codeStore.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Cleaned {} expired codes", removed);
        }
    }


    // ==================== Внутренний record для хранения данных кода ====================

    private record CodeData(Long userId, Long chatId, List<Long> cityIds, long expiresAt) {}
}