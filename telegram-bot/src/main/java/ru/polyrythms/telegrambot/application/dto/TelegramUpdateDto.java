package ru.polyrythms.telegrambot.application.dto;

import lombok.Builder;
import lombok.Value;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.polyrythms.telegrambot.domain.model.VoiceMessage;

@Value
@Builder
public class TelegramUpdateDto {

    // Информация о чате и пользователе
    Long chatId;
    Long userId;
    String username;
    String firstName;
    String lastName;
    boolean isGroupChat;
    Long groupChatId;  // Реальный ID группы (если это группа)

    // Текстовые данные
    String text;
    boolean hasText;
    boolean isCommand;
    String commandName;
    String[] commandArgs;

    // Голосовые данные
    String fileId;
    String fileUniqueId;
    Integer duration;
    boolean hasVoice;

    /**
     * Фабричный метод для создания DTO из Telegram Update
     */
    public static TelegramUpdateDto fromUpdate(Update update) {
        if (update == null || !update.hasMessage()) {
            return null;
        }

        Message message = update.getMessage();

        TelegramUpdateDtoBuilder builder = TelegramUpdateDto.builder()
                .chatId(message.getChatId())
                .userId(message.getFrom().getId())
                .username(message.getFrom().getUserName())
                .firstName(message.getFrom().getFirstName())
                .lastName(message.getFrom().getLastName())
                .isGroupChat(message.getChat().isGroupChat() || message.getChat().isSuperGroupChat());

        // ID группы (если это групповой чат)
        if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
            builder.groupChatId(message.getChatId());
        }

        // Обработка текста и команд
        if (message.hasText()) {
            String fullText = message.getText();
            builder.hasText(true)
                    .text(fullText);

            if (fullText.startsWith("/")) {
                builder.isCommand(true);

                String[] parts = fullText.split("\\s+");
                String command = parts[0];

                // Удаляем @botname из команды, если есть
                if (command.contains("@")) {
                    command = command.split("@")[0];
                }

                builder.commandName(command)
                        .commandArgs(parts);
            }
        }

        // Обработка голосовых сообщений
        if (message.hasVoice()) {
            builder.hasVoice(true)
                    .fileId(message.getVoice().getFileId())
                    .fileUniqueId(message.getVoice().getFileUniqueId())
                    .duration(message.getVoice().getDuration());
        }

        return builder.build();
    }

    /**
     * Конвертация в доменную модель VoiceMessage
     */
    public VoiceMessage toVoiceMessage() {
        if (!hasVoice) {
            throw new IllegalStateException("Cannot convert to VoiceMessage: no voice data");
        }

        return VoiceMessage.builder()
                .fileId(fileId)
                .fileUniqueId(fileUniqueId)
                .chatId(chatId)
                .userId(userId)
                .duration(duration)
                .isGroupChat(isGroupChat)
                .build();
    }

    /**
     * Конвертация в CommandContext
     */
    public CommandContext toCommandContext() {
        if (!isCommand) {
            throw new IllegalStateException("Cannot convert to CommandContext: not a command");
        }

        return CommandContext.builder()
                .chatId(chatId)
                .userId(userId)
                .username(username != null ? username : firstName)
                .command(commandName)
                .args(commandArgs)
                .fullText(text)
                .isPrivateChat(!isGroupChat)
                .build();
    }

    /**
     * Получение отображаемого имени пользователя
     */
    public String getUserDisplayName() {
        if (username != null && !username.isEmpty()) {
            return "@" + username;
        }
        if (firstName != null) {
            return firstName + (lastName != null ? " " + lastName : "");
        }
        return String.valueOf(userId);
    }
}