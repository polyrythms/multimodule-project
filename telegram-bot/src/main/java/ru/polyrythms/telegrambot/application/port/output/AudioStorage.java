package ru.polyrythms.telegrambot.application.port.output;

import java.io.File;

public interface AudioStorage {
    String storeAudio(File audioFile, String uniqueId);
    String getPublicUrl(String objectName);
}