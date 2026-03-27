package ru.polyrythms.telegrambot.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class VoiceMessageProcessingService implements VoiceMessageProcessingUseCase {

    private final GroupManagementUseCase groupManagementUseCase;
    private final AudioStorage audioStorage;
    private final TelegramFileDownloader fileDownloader;
    private final DecryptionTaskProducer taskProducer;
    private final MessageSender messageSender;

    @Override
    public void processVoiceMessage(VoiceMessage voiceMessage) {
        log.info("Processing voice message from chatId: {}", voiceMessage.getChatId());

        try {
            // Скачиваем голосовое сообщение
            File voiceFile = fileDownloader.downloadFile(voiceMessage.getFileId());

            // Сохраняем в MinIO
            String audioId = audioStorage.storeAudio(voiceFile, voiceMessage.getFileUniqueId());
            String audioUrl = audioStorage.getPublicUrl(audioId);

            // Создаем и отправляем задание
            DecryptionTask task = createDecryptionTask(audioId, voiceMessage.getChatId(), audioUrl);
            taskProducer.sendTask(task);

            // Отправляем подтверждение только в личных чатах
            if (!voiceMessage.getIsGroupChat()) {
                messageSender.sendMessage(voiceMessage.getChatId(), "✅ Ваше голосовое сообщение принято в обработку...");
            }

            log.info("Voice message processed successfully, taskId: {}", task.getTaskId());

        } catch (Exception e) {
            log.error("Failed to process voice message from chatId: {}", voiceMessage.getChatId(), e);
            messageSender.sendMessage(voiceMessage.getChatId(), "❌ Ошибка при обработке голосового сообщения");
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
}