// src/main/java/ru/polyrythms/telegrambot/infrastructure/adapter/out/telegram/TelegramFileDownloaderImpl.java

package ru.polyrythms.telegrambot.infrastructure.adapter.output.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.polyrythms.telegrambot.application.port.input.TelegramFileDownloader;

import java.io.File;

/**
 * Реализация outbound порта TelegramFileDownloader.
 * Использует TelegramBotClient для скачивания файлов из Telegram.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramFileDownloaderImpl implements TelegramFileDownloader {

    private final TelegramBotClient botClient;

    @Override
    public File downloadFile(String fileId) {
        try {
            log.debug("Downloading file: {}", fileId);
            return botClient.downloadFileById(fileId);
        } catch (Exception e) {
            log.error("Failed to download file: {}", fileId, e);
            throw new RuntimeException("Failed to download file: " + fileId, e);
        }
    }

    @Override
    public String getFilePath(String fileId) {
        try {
            var fileInfo = botClient.getTelegramFileInfo(fileId);
            log.debug("File path for {}: {}", fileId, fileInfo.getFilePath());
            return fileInfo.getFilePath();
        } catch (Exception e) {
            log.error("Failed to get file path for: {}", fileId, e);
            throw new RuntimeException("Failed to get file path for: " + fileId, e);
        }
    }
}