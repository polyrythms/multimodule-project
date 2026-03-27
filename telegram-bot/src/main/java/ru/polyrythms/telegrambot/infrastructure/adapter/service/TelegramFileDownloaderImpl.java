// infrastructure/adapter/service/TelegramFileDownloaderImpl.java
package ru.polyrythms.telegrambot.infrastructure.adapter.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.polyrythms.telegrambot.application.port.output.TelegramFileDownloader;
import ru.polyrythms.telegrambot.infrastructure.adapter.bot.TelegramBotWrapper;

import java.io.File;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramFileDownloaderImpl implements TelegramFileDownloader {

    private final TelegramBotWrapper botWrapper;

    @Override
    public File downloadFile(String fileId) {
        try {
            log.debug("Downloading file: {}", fileId);
            return botWrapper.downloadFileById(fileId);
        } catch (Exception e) {
            log.error("Failed to download file: {}", fileId, e);
            throw new RuntimeException("Failed to download file", e);
        }
    }

    @Override
    public String getFilePath(String fileId) {
        try {
            var fileInfo = botWrapper.getTelegramFileInfo(fileId);
            return fileInfo.getFilePath();
        } catch (Exception e) {
            log.error("Failed to get file path for: {}", fileId, e);
            throw new RuntimeException("Failed to get file path", e);
        }
    }
}