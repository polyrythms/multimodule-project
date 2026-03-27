// application/service/VoiceMessageProcessingService.java
package ru.polyrythms.telegrambot.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.polyrythms.telegrambot.application.port.input.GroupManagementUseCase;
import ru.polyrythms.telegrambot.application.port.input.VoiceMessageProcessingUseCase;
import ru.polyrythms.telegrambot.application.port.output.AudioStorage;
import ru.polyrythms.telegrambot.application.port.output.DecryptionTaskProducer;
import ru.polyrythms.telegrambot.application.port.output.MessageSender;
import ru.polyrythms.telegrambot.application.port.output.TelegramFileDownloader;
import ru.polyrythms.telegrambot.domain.model.DecryptionTask;
import ru.polyrythms.telegrambot.domain.model.VoiceMessage;

import java.io.File;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceMessageProcessingService implements VoiceMessageProcessingUseCase {

    private final GroupManagementUseCase groupManagementUseCase;
    private final AudioStorage audioStorage;
    private final TelegramFileDownloader fileDownloader;
    private final DecryptionTaskProducer taskProducer;
    private final MessageSender messageSender;

    @Override
    public void processVoiceMessage(VoiceMessage voiceMessage) {
        log.info("Processing voice message from chatId: {}, fileId: {}",
                voiceMessage.getChatId(), voiceMessage.getFileId());

        File voiceFile = null;
        try {
            // Валидация
            validateVoiceMessage(voiceMessage);

            // Скачиваем файл
            log.debug("Downloading file: {}", voiceMessage.getFileId());
            voiceFile = fileDownloader.downloadFile(voiceMessage.getFileId());

            if (voiceFile == null || !voiceFile.exists()) {
                throw new RuntimeException("Downloaded file is null or doesn't exist");
            }

            log.info("File downloaded: {} ({} bytes)",
                    voiceFile.getName(), voiceFile.length());

            // Сохраняем в MinIO
            String audioId = audioStorage.storeAudio(voiceFile, voiceMessage.getFileUniqueId());
            String audioUrl = audioStorage.getPublicUrl(audioId);

            log.debug("File stored in MinIO: {}, URL: {}", audioId, audioUrl);

            // Создаем и отправляем задание
            DecryptionTask task = createDecryptionTask(audioId, voiceMessage.getChatId(), audioUrl);
            taskProducer.sendTask(task);

            // Отправляем подтверждение
            if (!voiceMessage.getIsGroupChat()) {
                messageSender.sendMessageAsync(voiceMessage.getChatId(),
                        "✅ Ваше голосовое сообщение принято в обработку...");
            }

            log.info("Voice message processed successfully, taskId: {}", task.getTaskId());

        } catch (Exception e) {
            log.error("Failed to process voice message from chatId: {}",
                    voiceMessage.getChatId(), e);

            String errorMessage = getErrorMessage(e);
            if (!voiceMessage.getIsGroupChat()) {
                messageSender.sendMessage(voiceMessage.getChatId(),
                        "❌ Ошибка при обработке: " + errorMessage);
            }

        } finally {
            cleanUpTempFile(voiceFile);
        }
    }

    private void validateVoiceMessage(VoiceMessage voiceMessage) {
        if (voiceMessage.getFileId() == null || voiceMessage.getFileId().isEmpty()) {
            throw new IllegalArgumentException("File ID is null or empty");
        }

        if (voiceMessage.getChatId() == null) {
            throw new IllegalArgumentException("Chat ID is null");
        }
    }

    private DecryptionTask createDecryptionTask(String audioId, Long chatId, String audioUrl) {
        return DecryptionTask.builder()
                .taskId(UUID.randomUUID().toString())
                .audioId(audioId)
                .chatId(chatId)
                .audioUrl(audioUrl)
                .createdAt(LocalDateTime.now())
                .status(DecryptionTask.TaskStatus.CREATED)
                .build();
    }

    private String getErrorMessage(Exception e) {
        if (e instanceof IllegalArgumentException) {
            return e.getMessage();
        }
        if (e instanceof java.io.IOException) {
            return "Ошибка при работе с файлом";
        }
        return "Попробуйте позже";
    }

    private void cleanUpTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                if (file.delete()) {
                    log.debug("Temporary file deleted: {}", file.getAbsolutePath());
                } else {
                    log.warn("Failed to delete temporary file: {}", file.getAbsolutePath());
                }
            } catch (Exception e) {
                log.warn("Error deleting temporary file: {}", file.getAbsolutePath(), e);
            }
        }
    }
}