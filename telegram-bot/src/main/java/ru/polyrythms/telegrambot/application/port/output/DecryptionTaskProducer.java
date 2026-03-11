package ru.polyrythms.telegrambot.application.port.output;

import ru.polyrythms.telegrambot.domain.model.DecryptionTask;

public interface DecryptionTaskProducer {
    void sendTask(DecryptionTask task);
}