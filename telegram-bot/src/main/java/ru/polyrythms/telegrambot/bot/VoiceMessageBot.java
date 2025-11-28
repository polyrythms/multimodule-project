package ru.polyrythms.telegrambot.bot;

import lombok.SneakyThrows;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.polyrythms.kafka.Topics;
import ru.polyrythms.kafka.dto.AudioDecryptionTask;
import ru.polyrythms.telegrambot.config.TelegramBotConfig;
import ru.polyrythms.telegrambot.entity.AdminUser;
import ru.polyrythms.telegrambot.entity.TelegramGroup;
import ru.polyrythms.telegrambot.entity.AdminRole;
import ru.polyrythms.telegrambot.service.AdminService;
import ru.polyrythms.telegrambot.service.AudioStorageService;
import ru.polyrythms.telegrambot.service.TelegramGroupService;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class VoiceMessageBot extends TelegramLongPollingBot {

    private final TelegramBotConfig config;
    private final AudioStorageService audioStorageService;
    private final KafkaTemplate<String, AudioDecryptionTask> kafkaTemplate;
    private final TelegramGroupService groupService;
    private final AdminService adminService;

    private static final Pattern ADD_ADMIN_PATTERN = Pattern.compile("/addadmin\\s+(\\S+)\\s+(OWNER|ADMIN|MODERATOR)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REMOVE_ADMIN_PATTERN = Pattern.compile("/removeadmin\\s+(\\S+)");
    private static final Pattern ADD_GROUP_PATTERN = Pattern.compile("/addgroup\\s+(-?\\d+)\\s+(.+)");
    private static final Pattern REMOVE_GROUP_PATTERN = Pattern.compile("/(removegroup|deactivategroup)\\s+(-?\\d+)");
    private static final Pattern ACTIVATE_GROUP_PATTERN = Pattern.compile("/activategroup\\s+(-?\\d+)");

    public VoiceMessageBot(TelegramBotConfig config,
                           AudioStorageService audioStorageService,
                           KafkaTemplate<String, AudioDecryptionTask> kafkaTemplate,
                           TelegramGroupService groupService,
                           AdminService adminService) {
        super(config.getBotToken());
        this.config = config;
        this.audioStorageService = audioStorageService;
        this.kafkaTemplate = kafkaTemplate;
        this.groupService = groupService;
        this.adminService = adminService;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    @SneakyThrows
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            Long userId = message.getFrom().getId();

            // Обработка команд (только в личных чатах)
            if (message.hasText() && message.getText().startsWith("/") && !isGroupChat(message.getChat())) {
                handleCommand(message);
                return;
            }

            // Обработка голосовых сообщений в разрешенных группах
            if (message.hasVoice() && isGroupChat(message.getChat())) {
                if (groupService.isGroupAllowed(chatId)) {
                    handleVoiceMessage(message, chatId);
                }
                return;
            }

            // Обработка голосовых в личных чатах (для всех)
            if (message.hasVoice() && !isGroupChat(message.getChat())) {
                handleVoiceMessage(message, chatId);
            }
        }
    }

    private void handleCommand(Message message) throws TelegramApiException {
        String text = message.getText();
        Long chatId = message.getChatId();
        Long userId = message.getFrom().getId();
        String[] args = text.split("\\s+");
        String command = args[0].toLowerCase();

        switch (command) {
            case "/start":
                handleStartCommand(chatId, userId);
                break;
            case "/addadmin":
                handleAddAdminCommand(message, args, chatId, userId);
                break;
            case "/removeadmin":
                handleRemoveAdminCommand(message, args, chatId, userId);
                break;
            case "/addgroup":
                handleAddGroupCommand(message, args, chatId, userId);
                break;
            case "/removegroup":
            case "/deactivategroup":
                handleDeactivateGroupCommand(message, args, chatId, userId);
                break;
            case "/activategroup":
                handleActivateGroupCommand(message, args, chatId, userId);
                break;
            case "/listgroups":
                handleListGroupsCommand(chatId, userId);
                break;
            case "/listadmins":
                handleListAdminsCommand(chatId, userId);
                break;
            case "/groupinfo":
                handleGroupInfoCommand(message, chatId, userId);
                break;
            case "/stats":
                handleStatsCommand(chatId, userId);
                break;
            case "/help":
                handleHelpCommand(chatId, userId);
                break;
            case "/myrole":
                handleMyRoleCommand(chatId, userId);
                break;
            default:
                sendTextMessage(chatId, "❌ Неизвестная команда. Используйте /help для списка команд.");
        }
    }

    private void handleStartCommand(Long chatId, Long userId) throws TelegramApiException {
        String welcome = "🤖 Бот для обработки голосовых сообщений\n\n";

        if (adminService.isAdmin(userId)) {
            AdminRole role = adminService.getUserRole(userId);
            welcome += "👑 Ваша роль: " + role + "\n";
            welcome += "Используйте /help для просмотра доступных команд";
        } else {
            welcome += "Отправьте голосовое сообщение для обработки.\n";
            welcome += "Для доступа к админ-панели обратитесь к администратору.";
        }

        sendTextMessage(chatId, welcome);
    }

    private void handleAddAdminCommand(Message message, String[] args, Long chatId, Long userId) throws TelegramApiException {
        if (!adminService.canManageAdmins(userId)) {
            sendTextMessage(chatId, "❌ Недостаточно прав. Только владелец может добавлять администраторов.");
            return;
        }

        var matcher = ADD_ADMIN_PATTERN.matcher(message.getText());
        if (matcher.matches()) {
            try {
                String target = matcher.group(1);
                AdminRole role = AdminRole.valueOf(matcher.group(2).toUpperCase());

                Long targetUserId = parseUserId(target);
                String username = extractUsername(target);

                adminService.addAdmin(userId, targetUserId, username, role);
                sendTextMessage(chatId, "✅ Администратор добавлен: " + username + " (" + role + ")");

            } catch (Exception e) {
                sendTextMessage(chatId, "❌ Ошибка: " + e.getMessage());
            }
        } else {
            sendTextMessage(chatId, "❌ Использование: /addadmin @username_or_id ADMIN|MODERATOR");
        }
    }

    private void handleRemoveAdminCommand(Message message, String[] args, Long chatId, Long userId) throws TelegramApiException {
        if (!adminService.canManageAdmins(userId)) {
            sendTextMessage(chatId, "❌ Недостаточно прав. Только владелец может удалять администраторов.");
            return;
        }

        var matcher = REMOVE_ADMIN_PATTERN.matcher(message.getText());
        if (matcher.matches()) {
            try {
                String target = matcher.group(1);
                Long targetUserId = parseUserId(target);

                adminService.removeAdmin(userId, targetUserId);
                sendTextMessage(chatId, "✅ Администратор удален");

            } catch (Exception e) {
                sendTextMessage(chatId, "❌ Ошибка: " + e.getMessage());
            }
        } else {
            sendTextMessage(chatId, "❌ Использование: /removeadmin @username_or_id");
        }
    }

    private void handleAddGroupCommand(Message message, String[] args, Long chatId, Long userId) throws TelegramApiException {
        if (!adminService.canManageGroups(userId)) {
            sendTextMessage(chatId, "❌ Недостаточно прав для управления группами.");
            return;
        }

        var matcher = ADD_GROUP_PATTERN.matcher(message.getText());
        if (matcher.matches()) {
            try {
                Long groupChatId = Long.parseLong(matcher.group(1));
                String groupTitle = matcher.group(2);

                groupService.addGroup(groupChatId, groupTitle, userId);
                sendTextMessage(chatId, "✅ Группа добавлена: \"" + groupTitle + "\"");

            } catch (Exception e) {
                sendTextMessage(chatId, "❌ Ошибка: " + e.getMessage());
            }
        } else {
            sendTextMessage(chatId, "❌ Использование: /addgroup <chat_id> \"Название группы\"");
        }
    }

    private void handleDeactivateGroupCommand(Message message, String[] args, Long chatId, Long userId) throws TelegramApiException {
        if (!adminService.canManageGroups(userId)) {
            sendTextMessage(chatId, "❌ Недостаточно прав для управления группами.");
            return;
        }

        var matcher = REMOVE_GROUP_PATTERN.matcher(message.getText());
        if (matcher.matches()) {
            try {
                Long groupChatId = Long.parseLong(matcher.group(2));
                groupService.deactivateGroup(groupChatId, userId);
                sendTextMessage(chatId, "✅ Группа деактивирована");
            } catch (Exception e) {
                sendTextMessage(chatId, "❌ Ошибка: " + e.getMessage());
            }
        } else {
            sendTextMessage(chatId, "❌ Использование: /removegroup <chat_id>");
        }
    }

    private void handleActivateGroupCommand(Message message, String[] args, Long chatId, Long userId) throws TelegramApiException {
        if (!adminService.canManageGroups(userId)) {
            sendTextMessage(chatId, "❌ Недостаточно прав для управления группами.");
            return;
        }

        var matcher = ACTIVATE_GROUP_PATTERN.matcher(message.getText());
        if (matcher.matches()) {
            try {
                Long groupChatId = Long.parseLong(matcher.group(1));
                groupService.activateGroup(groupChatId, userId);
                sendTextMessage(chatId, "✅ Группа активирована");
            } catch (Exception e) {
                sendTextMessage(chatId, "❌ Ошибка: " + e.getMessage());
            }
        } else {
            sendTextMessage(chatId, "❌ Использование: /activategroup <chat_id>");
        }
    }

    private void handleListGroupsCommand(Long chatId, Long userId) throws TelegramApiException {
        if (!adminService.isAdmin(userId)) {
            sendTextMessage(chatId, "❌ Доступно только администраторам.");
            return;
        }

        List<TelegramGroup> groups = groupService.getAllGroups();
        if (groups.isEmpty()) {
            sendTextMessage(chatId, "📝 Список групп пуст");
            return;
        }

        StringBuilder response = new StringBuilder("📋 Список групп:\n\n");
        for (TelegramGroup group : groups) {
            response.append(group.getIsActive() ? "✅ " : "❌ ")
                    .append(group.getTitle())
                    .append("\nID: ")
                    .append(group.getChatId())
                    .append("\nДобавил: ")
                    .append(group.getAddedByUsername() != null ? group.getAddedByUsername() : group.getAddedBy())
                    .append("\n\n");
        }

        sendTextMessage(chatId, response.toString());
    }

    private void handleListAdminsCommand(Long chatId, Long userId) throws TelegramApiException {
        if (!adminService.canManageAdmins(userId)) {
            sendTextMessage(chatId, "❌ Недостаточно прав. Только владелец может просматривать список администраторов.");
            return;
        }

        List<AdminUser> admins = adminService.getAllAdmins();
        StringBuilder response = new StringBuilder("👑 Список администраторов:\n\n");

        for (AdminUser admin : admins) {
            response.append(admin.getRole() == AdminRole.OWNER ? "👑 " : "🔧 ")
                    .append(admin.getUsername())
                    .append(" (ID: ")
                    .append(admin.getUserId())
                    .append(")\nРоль: ")
                    .append(admin.getRole())
                    .append("\n\n");
        }

        sendTextMessage(chatId, response.toString());
    }

    private void handleGroupInfoCommand(Message message, Long chatId, Long userId) throws TelegramApiException {
        Chat chat = message.getChat();
        if (!isGroupChat(chat)) {
            sendTextMessage(chatId, "ℹ️ Эта команда работает только в группах");
            return;
        }

        String info = String.format(
                """
                        📋 Информация о группе:
                        
                        Название: %s
                        Chat ID: %d
                        Тип: %s
                        
                        Для добавления в белый список используйте:
                        /addgroup %d "%s\"""",
                chat.getTitle(),
                chat.getId(),
                chat.isSuperGroupChat() ? "супергруппа" : "группа",
                chat.getId(),
                chat.getTitle()
        );

        sendTextMessage(chatId, info);
    }

    private void handleStatsCommand(Long chatId, Long userId) throws TelegramApiException {
        if (!adminService.isAdmin(userId)) {
            sendTextMessage(chatId, "❌ Доступно только администраторам.");
            return;
        }

        long totalGroups = groupService.getActiveGroupsCount();
        long userGroups = groupService.getUserGroupsCount(userId);
        long totalAdmins = adminService.getAdminCount();

        String stats = String.format(
                """
                        📊 Статистика:
                        
                        Активных групп: %d
                        Ваших групп: %d
                        Всего администраторов: %d""",
                totalGroups, userGroups, totalAdmins
        );

        sendTextMessage(chatId, stats);
    }

    private void handleHelpCommand(Long chatId, Long userId) throws TelegramApiException {
        StringBuilder help = new StringBuilder("📖 Доступные команды:\n\n");

        if (adminService.isAdmin(userId)) {
            AdminRole role = adminService.getUserRole(userId);
            help.append("👑 Администраторские команды (").append(role).append("):\n");

            if (adminService.canManageAdmins(userId)) {
                help.append("/addadmin @user ROLE - Добавить администратора\n");
                help.append("/removeadmin @user - Удалить администратора\n");
                help.append("/listadmins - Список администраторов\n");
            }

            if (adminService.canManageGroups(userId)) {
                help.append("/addgroup <id> \"name\" - Добавить группу\n");
                help.append("/removegroup <id> - Удалить группу\n");
                help.append("/activategroup <id> - Активировать группу\n");
            }

            help.append("/listgroups - Список групп\n");
            help.append("/stats - Статистика\n");
            help.append("/myrole - Показать свою роль\n");
        }

        help.append("/groupinfo - Информация о группе (в группе)\n");
        help.append("/help - Эта справка\n");

        sendTextMessage(chatId, help.toString());
    }

    private void handleMyRoleCommand(Long chatId, Long userId) throws TelegramApiException {
        if (adminService.isAdmin(userId)) {
            AdminRole role = adminService.getUserRole(userId);
            sendTextMessage(chatId, "👑 Ваша роль: " + role);
        } else {
            sendTextMessage(chatId, "ℹ️ Вы не являетесь администратором");
        }
    }

    // Вспомогательные методы
    private Long parseUserId(String input) {
        if (input.startsWith("@")) {
            // В реальном проекте здесь бы был поиск пользователя по username
            // Для простоты возвращаем хэш от username
            return (long) input.hashCode();
        }
        return Long.parseLong(input);
    }

    private String extractUsername(String input) {
        if (input.startsWith("@")) {
            return input;
        }
        return "user_" + input;
    }

    private boolean isGroupChat(Chat chat) {
        return chat.isGroupChat() || chat.isSuperGroupChat();
    }

    @SneakyThrows
    private void handleVoiceMessage(Message message, Long chatId) {
        Voice voice = message.getVoice();

        // Скачиваем голосовое сообщение
        GetFile getFile = new GetFile();
        getFile.setFileId(voice.getFileId());
        String filePath = execute(getFile).getFilePath();
        File voiceFile = downloadFile(filePath);

        // Сохраняем в MinIO
        String audioId = audioStorageService.storeAudio(voiceFile, voice.getFileUniqueId());
        String audioUrl = audioStorageService.getPublicAudioUrl(audioId);

        // Отправляем задание в Kafka
        AudioDecryptionTask task = AudioDecryptionTask.createVoiceTask(audioId, chatId, audioUrl);
        kafkaTemplate.send(Topics.AUDIO_DECRYPTION_REQUESTS, task.getTaskId(), task);

        if (!isGroupChat(message.getChat())) {
            // Отправляем подтверждение
            sendTextMessage(chatId, "✅ Ваше голосовое сообщение принято в обработку...");
        }
    }

    public void sendTextMessage(Long chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        execute(message);
    }
}