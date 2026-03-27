package ru.polyrythms.telegrambot.infrastructure.adapter.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import ru.polyrythms.telegrambot.application.port.output.TelegramFileDownloader;

import java.io.File;

@Slf4j
@Component
public class TelegramFileDownloaderImpl implements TelegramFileDownloader {

    private DefaultAbsSender telegramBot;

    // Используем setter injection вместо constructor injection
    @Autowired
    public void setTelegramBot(@Lazy DefaultAbsSender telegramBot) {
        this.telegramBot = telegramBot;
    }
    @Override
    @SneakyThrows
    public File downloadFile(String fileId) {
        if (telegramBot == null) {
            throw new IllegalStateException("Telegram bot not initialized yet");
        }

        try {
            GetFile getFile = new GetFile();
            getFile.setFileId(fileId);
            String filePath = telegramBot.execute(getFile).getFilePath();
            return telegramBot.downloadFile(filePath);
        } catch (Exception e) {
            log.error("Failed to download file: {}", fileId, e);
            throw new RuntimeException("Failed to download file", e);
        }
    }

    @Override
    @SneakyThrows
    public String getFilePath(String fileId) {
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);
        return telegramBot.execute(getFile).getFilePath();
    }
}