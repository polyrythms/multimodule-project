package ru.polyrythms.audioservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.polyrythms.audioservice.json.assemblyai.TranscriptionRequest;
import ru.polyrythms.audioservice.json.assemblyai.TranscriptionResponse;
import ru.polyrythms.audioservice.json.assemblyai.TranscriptionStatus;
import ru.polyrythms.audioservice.json.assemblyai.UploadResponse;

import java.time.Duration;

/**
 * Сервис для взаимодействия с AssemblyAI API для транскрипции аудио.
 *
 * <p>Этот сервис предоставляет реактивные методы для:
 * <ul>
 *   <li>Загрузки аудио файлов в AssemblyAI</li>
 *   <li>Отправки запросов на транскрипцию</li>
 *   <li>Опроса статуса транскрипции до завершения</li>
 *   <li>Получения распознанного текста</li>
 * </ul>
 *
 * <p><b>Процесс работы с AssemblyAI:</b>
 * <ol>
 *   <li>Аудио файл загружается из MinIO</li>
 *   <li>Файл загружается в AssemblyAI через endpoint /upload</li>
 *   <li>Полученный URL загруженного файла используется для создания запроса транскрипции</li>
 *   <li>Запрос на транскрипцию отправляется через endpoint /transcript</li>
 *   <li>Сервис периодически опрашивает статус транскрипции через endpoint /transcript/{id}</li>
 *   <li>Когда статус становится "completed", возвращается распознанный текст</li>
 * </ol>
 *
 * <p>Сервис использует экспоненциальный бэк-офф для повторных попыток и таймауты
 * для предотвращения бесконечного ожидания.
 *
 * @see MinioService
 * @see <a href="https://www.assemblyai.com/docs">AssemblyAI Documentation</a>
 */
@Service
@Slf4j
public class ReactiveAssemblyAIService {

    private final WebClient webClient;
    private final MinioService minioService;

    public ReactiveAssemblyAIService(
            WebClient webClient,
            MinioService minioService) {

        this.webClient = webClient;
        this.minioService = minioService;
    }

    /**
     * Выполняет полный цикл транскрипции аудио файла.
     *
     * <p><b>Процесс выполнения:</b>
     * <ol>
     *   <li>Загружает аудио файл из MinIO по audioId</li>
     *   <li>Загружает аудио данные в AssemblyAI</li>
     *   <li>Отправляет запрос на транскрипцию</li>
     *   <li>Опросит статус транскрипции до завершения</li>
     *   <li>Возвращает распознанный текст</li>
     * </ol>
     *
     * @param audioId идентификатор аудио файла в MinIO
     * @return Mono с распознанным текстом
     * @throws RuntimeException если транскрипция завершилась ошибкой или превышен таймаут
     * @see #uploadToAssemblyAI(byte[])
     * @see #submitTranscription(String)
     * @see #pollTranscriptionResult(String)
     */
    public Mono<String> transcribeAudio(String audioId) {
        return minioService.downloadAudio(audioId)
                .flatMap(this::uploadToAssemblyAI)
                .flatMap(this::submitTranscription)
                .flatMap(this::pollTranscriptionResult)
                .timeout(Duration.ofMinutes(10))
                .onErrorResume(e -> Mono.error(new RuntimeException("Transcription failed: " + e.getMessage(), e)));
    }

    /**
     * Загружает аудио данные в AssemblyAI и возвращает URL загруженного файла.
     *
     * <p><b>Процесс загрузки:</b>
     * <ol>
     *   <li>Выполняет POST запрос к эндпоинту /upload AssemblyAI API</li>
     *   <li>Передает бинарные данные аудио файла в теле запроса</li>
     *   <li>Устанавливает необходимые заголовки для аутентификации и типа контента</li>
     *   <li>Обрабатывает ответ от сервера и извлекает upload_url</li>
     * </ol>
     *
     * <p><b>Стратегия повторных попыток:</b>
     * <ul>
     *   <li>При возникновении ошибки выполняет до 3 повторных попыток</li>
     *   <li>Использует экспоненциальный бэк-офф с начальной задержкой 2 секунды</li>
     *   <li>Максимальная задержка между попытками: 10 секунд</li>
     *   <li>Добавляет jitter (случайность) для предотвращения "толпы" запросов</li>
     * </ul>
     *
     * <p><b>Обработка ошибок:</b>
     * <ul>
     *   <li>Логирует успешную загрузку с полученным URL</li>
     *   <li>Логирует ошибки при неудачных попытках загрузки</li>
     *   <li>При исчерпании всех попыток генерирует исключение с информацией о количестве попыток</li>
     * </ul>
     *
     * <p><b>Таймауты:</b>
     * <ul>
     *   <li>Общий таймаут операции: 30 секунд</li>
     * </ul>
     *
     * @param audioData бинарные данные аудио файла для загрузки
     * @return Mono с URL загруженного аудио файла в AssemblyAI
     * @throws RuntimeException если загрузка не удалась после всех попыток или превышен таймаут
     * @see <a href="https://www.assemblyai.com/docs/audio-inputs#uploading-files-via-our-upload-endpoint">
     * AssemblyAI Upload Documentation</a>
     * @see WebClient
     * @see Retry
     */
    private Mono<String> uploadToAssemblyAI(byte[] audioData) {
        return webClient.post()
                .uri("/upload")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .bodyValue(audioData)
                .retrieve()
                .bodyToMono(UploadResponse.class)
                .map(UploadResponse::getUploadUrl)
                .timeout(Duration.ofSeconds(30))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .jitter(0.5)
                        .maxBackoff(Duration.ofSeconds(10))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            throw new RuntimeException("Upload failed after " + retrySignal.totalRetries() + " retries");
                        }))
                .doOnSuccess(url -> log.info("Successfully uploaded audio to AssemblyAI, URL: {}", url))
                .doOnError(e -> log.error("Failed to upload audio to AssemblyAI", e));
    }

    /**
     * Отправляет запрос на транскрипцию загруженного аудио файла.
     *
     * <p>Использует endpoint POST /transcript для создания задачи транскрипции.
     * Указывает язык распознавания как русский. В случае ошибки выполняет до 3
     * повторных попыток с экспоненциальным бэк-оффом.
     *
     * @param assemblyAiAudioUrl URL аудио файла, полученный после загрузки в AssemblyAI
     * @return Mono с идентификатором транскрипции (transcriptId)
     * @throws RuntimeException если отправка запроса не удалась после всех попыток
     * @see <a href="https://www.assemblyai.com/docs/transcription#requesting-a-transcription">
     * AssemblyAI Transcription Request Documentation</a>
     */
    private Mono<String> submitTranscription(String assemblyAiAudioUrl) {
        TranscriptionRequest request = new TranscriptionRequest(assemblyAiAudioUrl, TranscriptionRequest.LanguageCode.ru);

        return webClient.post()
                .uri("/transcript")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(TranscriptionResponse.class)
                .map(TranscriptionResponse::getId)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));
    }

    /**
     * Опросит статус транскрипции до завершения или ошибки.
     *
     * <p><b>Логика опроса:</b>
     * <ul>
     *   <li>Проверяет статус транскрипции каждые 2-10 секунд (с экспоненциальным бэк-оффом)</li>
     *   <li>Максимум 60 попыток опроса</li>
     *   <li>Общий таймаут операции: 10 минут</li>
     *   <li>При статусе "completed" возвращает распознанный текст</li>
     *   <li>При статусе "error" выбрасывает исключение</li>
     *   <li>При других статусах ("processing", "queued") продолжает опрос</li>
     * </ul>
     *
     * @param transcriptId идентификатор транскрипции, полученный при создании запроса
     * @return Mono с распознанным текстом
     * @throws RuntimeException если транскрипция завершилась ошибкой, превышен таймаут или лимит попыток
     * @see #getTranscriptionStatus(String)
     * @see <a href="https://www.assemblyai.com/docs/transcription#polling-for-transcription-results">
     * AssemblyAI Polling Documentation</a>
     */
    private Mono<String> pollTranscriptionResult(String transcriptId) {
        log.debug("Starting to poll transcription result for transcriptId: {}", transcriptId);

        return Mono.defer(() -> getTranscriptionStatus(transcriptId))
                .doOnNext(status -> {
                    log.debug("Polling status for {}: {}", transcriptId, status.getStatus());
                    // Детальное логирование полученного статуса
                    log.debug("Transcription status details - text: {}, confidence: {}, error: {}",
                            status.getText() != null ? "length=" + status.getText().length() : "null",
                            status.getConfidence(),
                            status.getError());
                })
                .flatMap(status -> {
                    switch (status.getStatus().toLowerCase()) {
                        case "completed":
                            log.info("🎉 Transcription completed for transcriptId: {}, confidence: {}",
                                    transcriptId, status.getConfidence());

                            // Детальное логирование результата
                            if (status.getText() != null && !status.getText().trim().isEmpty()) {
                                log.info("📝 Transcription text ({} chars): '{}'",
                                        status.getText().length(),
                                        status.getText().length() > 100 ?
                                                status.getText().substring(0, 100) + "..." : status.getText());
                            } else {
                                log.warn("⚠️ Transcription text is empty or null!");
                            }

                            return Mono.just(status.getText());
                        case "error":
                            String errorMsg = "Transcription error: " + status.getError();
                            log.error("❌ Transcription failed for transcriptId: {}, error: {}", transcriptId, status.getError());
                            return Mono.error(new RuntimeException(errorMsg));
                        default:
                            log.debug("⏳ Transcription in progress for transcriptId: {}, status: {}",
                                    transcriptId, status.getStatus());
                            return Mono.empty();
                    }
                })
                .repeatWhenEmpty(repeat -> repeat
                        .zipWith(Flux.range(1, 60), (signal, index) -> index)
                        .doOnNext(index -> log.trace("Poll attempt {} for transcriptId: {}", index, transcriptId))
                        .flatMap(index -> {
                            long delay = Math.min(2000 * index, 10000);
                            return Mono.delay(Duration.ofMillis(delay));
                        })
                )
                .timeout(Duration.ofMinutes(10))
                .switchIfEmpty(Mono.error(new RuntimeException("Transcription timeout for transcriptId: " + transcriptId)))
                .doOnError(e -> log.error("Polling failed for transcriptId: {}", transcriptId, e));
    }

    /**
     * Получает текущий статус транскрипции из AssemblyAI.
     *
     * <p>Использует endpoint GET /transcript/{id} для получения информации
     * о статусе транскрипции, включая текст, confidence score и возможные ошибки.
     *
     * @param transcriptId идентификатор транскрипции
     * @return Mono с объектом TranscriptionStatus, содержащим текущий статус и данные
     * @see <a href="https://www.assemblyai.com/docs/transcription#getting-the-transcription-result">
     * AssemblyAI Get Transcript Documentation</a>
     */
    private Mono<TranscriptionStatus> getTranscriptionStatus(String transcriptId) {
        return webClient.get()
                .uri("/transcript/" + transcriptId)
                .retrieve()
                .bodyToMono(TranscriptionStatus.class);
    }
}