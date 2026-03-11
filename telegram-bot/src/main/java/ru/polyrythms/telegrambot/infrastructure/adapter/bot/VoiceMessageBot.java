package ru.polyrythms.telegrambot.infrastructure.adapter.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.polyrythms.telegrambot.application.dto.CommandContext;
import ru.polyrythms.telegrambot.application.port.input.CommandHandlingUseCase;
import ru.polyrythms.telegrambot.application.port.input.GroupManagementUseCase;
import ru.polyrythms.telegrambot.application.port.input.VoiceMessageProcessingUseCase;
import ru.polyrythms.telegrambot.infrastructure.config.TelegramBotConfig;
import ru.polyrythms.telegrambot.domain.model.VoiceMessage;

@Slf4j
@Component
public class VoiceMessageBot extends TelegramLongPollingBot {

    private final TelegramBotConfig config;
    private final VoiceMessageProcessingUseCase voiceProcessingUseCase;
    private final CommandHandlingUseCase commandHandlingUseCase;
    private final GroupManagementUseCase groupManagementUseCase;

    public VoiceMessageBot(
            TelegramBotConfig config,
            VoiceMessageProcessingUseCase voiceProcessingUseCase,
            CommandHandlingUseCase commandHandlingUseCase,
            GroupManagementUseCase groupManagementUseCase) {
        super(config.getBotToken());
        this.config = config;
        this.voiceProcessingUseCase = voiceProcessingUseCase;
        this.commandHandlingUseCase = commandHandlingUseCase;
        this.groupManagementUseCase = groupManagementUseCase;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            return;
        }

        Message message = update.getMessage();
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        boolean isGroupChat = isGroupChat(message.getChat());

        log.debug("Received update from user: {} in chat: {}", userId, chatId);

        try {
            // Обработка команд (только в личных чатах)
            if (message.hasText() && message.getText().startsWith("/") && !isGroupChat) {
                handleCommand(message, chatId, userId);
                return;
            }

            // Обработка голосовых сообщений
            if (message.hasVoice()) {
                handleVoiceMessage(message, chatId, userId, isGroupChat);
            }

        } catch (Exception e) {
            log.error("Error processing update from user: {} in chat: {}", userId, chatId, e);
        }
    }

    private void handleCommand(Message message, Long chatId, Long userId) {
        String text = message.getText();
        String[] args = text.split("\\s+");
        String command = args[0];

        CommandContext context = CommandContext.builder()
                .chatId(chatId)
                .userId(userId)
                .command(command)
                .args(args)
                .fullText(text)
                .username(message.getFrom().getUserName())
                .isPrivateChat(true)
                .build();

        commandHandlingUseCase.handleCommand(chatId, userId, command, args, text);
    }

    private void handleVoiceMessage(Message message, Long chatId, Long userId, boolean isGroupChat) {
        // Проверяем, разрешена ли группа (для групповых чатов)
        if (isGroupChat && !groupManagementUseCase.isGroupAllowed(chatId)) {
            log.debug("Group chat {} is not allowed", chatId);
            return;
        }

        VoiceMessage voiceMessage = VoiceMessage.builder()
                .fileId(message.getVoice().getFileId())
                .fileUniqueId(message.getVoice().getFileUniqueId())
                .chatId(chatId)
                .userId(userId)
                .duration(message.getVoice().getDuration())
                .isGroupChat(isGroupChat)
                .build();

        voiceProcessingUseCase.processVoiceMessage(voiceMessage);
    }

    private boolean isGroupChat(Chat chat) {
        return chat.isGroupChat() || chat.isSuperGroupChat();
    }
}