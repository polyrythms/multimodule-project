package ru.polyrythms.telegrambot.infrastructure.adapter.service;

import lombok.SneakyThrows;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import ru.polyrythms.telegrambot.application.port.output.TelegramFileDownloader;

import java.io.File;

@Component
public class TelegramFileDownloaderImpl implements TelegramFileDownloader {

    private final DefaultAbsSender telegramBot;

    public TelegramFileDownloaderImpl(@Lazy DefaultAbsSender telegramBot) {
        this.telegramBot = telegramBot;
    }


    @Override
    @SneakyThrows
    public File downloadFile(String fileId) {
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);
        String filePath = telegramBot.execute(getFile).getFilePath();
        return telegramBot.downloadFile(filePath);
    }

    @Override
    @SneakyThrows
    public String getFilePath(String fileId) {
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);
        return telegramBot.execute(getFile).getFilePath();
    }
}