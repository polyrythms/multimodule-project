package ru.polyrythms.telegrambot.application.port.input;

import ru.polyrythms.telegrambot.domain.model.VoiceMessage;

public interface VoiceMessageProcessingUseCase {
    void processVoiceMessage(VoiceMessage voiceMessage);
}