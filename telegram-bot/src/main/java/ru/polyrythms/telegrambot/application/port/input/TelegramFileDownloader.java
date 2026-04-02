package ru.polyrythms.telegrambot.application.port.input;

import java.io.File;

public interface TelegramFileDownloader {
    File downloadFile(String fileId);
    String getFilePath(String fileId);
}