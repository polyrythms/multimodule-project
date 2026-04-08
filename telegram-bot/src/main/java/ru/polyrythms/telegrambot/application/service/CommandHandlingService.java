package ru.polyrythms.telegrambot.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import ru.polyrythms.telegrambot.application.dto.AdminUserDto;
import ru.polyrythms.telegrambot.application.dto.TelegramGroupDto;
import ru.polyrythms.telegrambot.application.port.input.AdminManagementUseCase;
import ru.polyrythms.telegrambot.application.port.input.CommandHandlingUseCase;
import ru.polyrythms.telegrambot.application.port.input.GroupManagementUseCase;
import ru.polyrythms.telegrambot.application.port.output.MessageSender;
import ru.polyrythms.telegrambot.domain.model.AdminRole;
import ru.polyrythms.telegrambot.domain.model.AdminUser;
import ru.polyrythms.telegrambot.domain.model.TelegramGroup;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class CommandHandlingService implements CommandHandlingUseCase {

    private final AdminManagementUseCase adminManagementUseCase;
    private final GroupManagementUseCase groupManagementUseCase;
    private final MessageSender messageSender;

    private static final Pattern ADD_ADMIN_PATTERN = Pattern.compile("/addadmin\\s+(\\S+)\\s+(OWNER|ADMIN|MODERATOR)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REMOVE_ADMIN_PATTERN = Pattern.compile("/removeadmin\\s+(\\S+)");
    private static final Pattern ADD_GROUP_PATTERN = Pattern.compile("/addgroup\\s+(-?\\d+)\\s+(.+)");
    private static final Pattern REMOVE_GROUP_PATTERN = Pattern.compile("/(removegroup|deactivategroup)\\s+(-?\\d+)");
    private static final Pattern ACTIVATE_GROUP_PATTERN = Pattern.compile("/activategroup\\s+(-?\\d+)");

    @Value("${weather.webapp.url}")
    private String weatherWebAppUrl;

    @Override
    public void handleCommand(Long chatId, Long userId, String command, String[] args, String fullText) {
        log.debug("Handling command: {} from user: {} in chat: {}", command, userId, chatId);

        try {
            switch (command.toLowerCase()) {
                case "/start":
                    handleStartCommand(chatId, userId);
                    break;
                case "/addadmin":
                    handleAddAdminCommand(chatId, userId, fullText);
                    break;
                case "/removeadmin":
                    handleRemoveAdminCommand(chatId, userId, fullText);
                    break;
                case "/addgroup":
                    handleAddGroupCommand(chatId, userId, fullText);
                    break;
                case "/removegroup":
                case "/deactivategroup":
                    handleDeactivateGroupCommand(chatId, userId, fullText);
                    break;
                case "/activategroup":
                    handleActivateGroupCommand(chatId, userId, fullText);
                    break;
                case "/listgroups":
                    handleListGroupsCommand(chatId, userId);
                    break;
                case "/listadmins":
                    handleListAdminsCommand(chatId, userId);
                    break;
                case "/groupinfo":
                    handleGroupInfoCommand(chatId, userId);
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
                    messageSender.sendMessage(chatId, "❌ Неизвестная команда. Используйте /help для списка команд.");
            }
        } catch (Exception e) {
            log.error("Error handling command: {}", command, e);
            messageSender.sendMessage(chatId, "❌ Ошибка при выполнении команды: " + e.getMessage());
        }
    }

    private void handleStartCommand(Long chatId, Long userId) {
        String welcome = "🤖 Бот для обработки голосовых сообщений\n\n";

        if (adminManagementUseCase.isAdmin(userId)) {
            AdminRole role = adminManagementUseCase.getUserRole(userId);
            welcome += "👑 Ваша роль: " + role + "\n";
            welcome += "Используйте /help для просмотра доступных команд";
        } else {
            welcome += "Отправьте голосовое сообщение для обработки.\n";
            welcome += "Для доступа к админ-панели обратитесь к администратору.";
        }

        messageSender.sendMessage(chatId, welcome);
    }

    private void handleAddAdminCommand(Long chatId, Long userId, String fullText) {
        Matcher matcher = ADD_ADMIN_PATTERN.matcher(fullText);
        if (!matcher.matches()) {
            messageSender.sendMessage(chatId, "❌ Использование: /addadmin @username_or_id ADMIN|MODERATOR");
            return;
        }

        try {
            String target = matcher.group(1);
            AdminRole role = AdminRole.valueOf(matcher.group(2).toUpperCase());

            Long targetUserId = parseUserId(target);
            String username = extractUsername(target);

            AdminUser newAdmin = adminManagementUseCase.addAdmin(userId, targetUserId, username, role);
            messageSender.sendMessage(chatId, "✅ Администратор добавлен: " + username + " (" + role + ")");

        } catch (Exception e) {
            messageSender.sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }

    private void handleRemoveAdminCommand(Long chatId, Long userId, String fullText) {
        Matcher matcher = REMOVE_ADMIN_PATTERN.matcher(fullText);
        if (!matcher.matches()) {
            messageSender.sendMessage(chatId, "❌ Использование: /removeadmin @username_or_id");
            return;
        }

        try {
            String target = matcher.group(1);
            Long targetUserId = parseUserId(target);

            adminManagementUseCase.removeAdmin(userId, targetUserId);
            messageSender.sendMessage(chatId, "✅ Администратор удален");

        } catch (Exception e) {
            messageSender.sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }

    private void handleAddGroupCommand(Long chatId, Long userId, String fullText) {
        Matcher matcher = ADD_GROUP_PATTERN.matcher(fullText);
        if (!matcher.matches()) {
            messageSender.sendMessage(chatId, "❌ Использование: /addgroup <chat_id> \"Название группы\"");
            return;
        }

        try {
            Long groupChatId = Long.parseLong(matcher.group(1));
            String groupTitle = matcher.group(2);

            TelegramGroup group = groupManagementUseCase.addGroup(groupChatId, groupTitle, userId);
            messageSender.sendMessage(chatId, "✅ Группа добавлена: \"" + groupTitle + "\"");

        } catch (Exception e) {
            messageSender.sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }

    private void handleDeactivateGroupCommand(Long chatId, Long userId, String fullText) {
        Matcher matcher = REMOVE_GROUP_PATTERN.matcher(fullText);
        if (!matcher.matches()) {
            messageSender.sendMessage(chatId, "❌ Использование: /removegroup <chat_id>");
            return;
        }

        try {
            Long groupChatId = Long.parseLong(matcher.group(2));
            groupManagementUseCase.deactivateGroup(groupChatId, userId);
            messageSender.sendMessage(chatId, "✅ Группа деактивирована");
        } catch (Exception e) {
            messageSender.sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }

    private void handleActivateGroupCommand(Long chatId, Long userId, String fullText) {
        Matcher matcher = ACTIVATE_GROUP_PATTERN.matcher(fullText);
        if (!matcher.matches()) {
            messageSender.sendMessage(chatId, "❌ Использование: /activategroup <chat_id>");
            return;
        }

        try {
            Long groupChatId = Long.parseLong(matcher.group(1));
            groupManagementUseCase.activateGroup(groupChatId, userId);
            messageSender.sendMessage(chatId, "✅ Группа активирована");
        } catch (Exception e) {
            messageSender.sendMessage(chatId, "❌ Ошибка: " + e.getMessage());
        }
    }

    private void handleListGroupsCommand(Long chatId, Long userId) {
        if (!adminManagementUseCase.isAdmin(userId)) {
            messageSender.sendMessage(chatId, "❌ Доступно только администраторам.");
            return;
        }

        List<TelegramGroup> groups = groupManagementUseCase.getAllGroups();
        if (groups.isEmpty()) {
            messageSender.sendMessage(chatId, "📝 Список групп пуст");
            return;
        }

        List<TelegramGroupDto> groupDtos = groups.stream()
                .map(TelegramGroupDto::fromDomain)
                .collect(Collectors.toList());

        StringBuilder response = new StringBuilder("📋 Список групп:\n\n");
        for (TelegramGroupDto group : groupDtos) {
            response.append(group.getIsActive() ? "✅ " : "❌ ")
                    .append(group.getTitle())
                    .append("\nID: ")
                    .append(group.getChatId())
                    .append("\nДобавил: ")
                    .append(group.getAddedByUsername())
                    .append("\n\n");
        }

        messageSender.sendMessage(chatId, response.toString());
    }

    private void handleListAdminsCommand(Long chatId, Long userId) {
        AdminUser admin = adminManagementUseCase.getAdmin(userId);
        if (admin == null || !admin.isOwner()) {
            messageSender.sendMessage(chatId, "❌ Недостаточно прав. Только владелец может просматривать список администраторов.");
            return;
        }

        List<AdminUser> admins = adminManagementUseCase.getAllAdmins();
        List<AdminUserDto> adminDtos = admins.stream()
                .map(AdminUserDto::fromDomain)
                .collect(Collectors.toList());

        StringBuilder response = new StringBuilder("👑 Список администраторов:\n\n");
        for (AdminUserDto adminDto : adminDtos) {
            response.append(adminDto.getRole() == AdminRole.OWNER ? "👑 " : "🔧 ")
                    .append(adminDto.getUsername())
                    .append(" (ID: ")
                    .append(adminDto.getUserId())
                    .append(")\nРоль: ")
                    .append(adminDto.getRole())
                    .append("\n\n");
        }

        messageSender.sendMessage(chatId, response.toString());
    }

    private void handleGroupInfoCommand(Long chatId, Long userId) {
        messageSender.sendMessage(chatId, "ℹ️ Эта команда временно недоступна");
    }

    private void handleStatsCommand(Long chatId, Long userId) {
        if (!adminManagementUseCase.isAdmin(userId)) {
            messageSender.sendMessage(chatId, "❌ Доступно только администраторам.");
            return;
        }

        long totalGroups = groupManagementUseCase.getActiveGroupsCount();
        long userGroups = groupManagementUseCase.getUserGroupsCount(userId);
        long totalAdmins = adminManagementUseCase.getAdminCount();

        String stats = String.format(
                """
                        📊 Статистика:
                        
                        Активных групп: %d
                        Ваших групп: %d
                        Всего администраторов: %d""",
                totalGroups, userGroups, totalAdmins
        );

        messageSender.sendMessage(chatId, stats);
    }

    private void handleHelpCommand(Long chatId, Long userId) {
        StringBuilder help = new StringBuilder("📖 Доступные команды:\n\n");

        if (adminManagementUseCase.isAdmin(userId)) {
            AdminRole role = adminManagementUseCase.getUserRole(userId);
            help.append("👑 Администраторские команды (").append(role).append("):\n");

            AdminUser admin = adminManagementUseCase.getAdmin(userId);
            if (admin != null && admin.isOwner()) {
                help.append("/addadmin @user ROLE - Добавить администратора\n");
                help.append("/removeadmin @user - Удалить администратора\n");
                help.append("/listadmins - Список администраторов\n");
            }

            if (role.canManageGroups()) {
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

        messageSender.sendMessage(chatId, help.toString());
    }

    private void handleMyRoleCommand(Long chatId, Long userId) {
        if (adminManagementUseCase.isAdmin(userId)) {
            AdminRole role = adminManagementUseCase.getUserRole(userId);
            messageSender.sendMessage(chatId, "👑 Ваша роль: " + role);
        } else {
            messageSender.sendMessage(chatId, "ℹ️ Вы не являетесь администратором");
        }
    }



//    private void handleWeatherCommand(Long chatId, Long userId, String[] args) {
//        // Проверка группы
//        if (!groupManagementUseCase.isGroupAllowed(chatId)) {
//            messageSender.sendMessage(chatId, "❌ Эта группа не активирована для прогноза погоды.");
//            return;
//        }
//
//        // Генерация JWT токена
//        String token = jwtTokenProvider.generateToken(userId, chatId,
//                adminService.isAdmin(userId) ? "admin" : "member");
//
//        // URL Web App на GitHub Pages с токеном
//        String webAppUrl = weatherWebAppUrl + "/?token=" + token;
//
//        // Создаем кнопку
//        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
//        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
//
//        InlineKeyboardButton button = new InlineKeyboardButton();
//        button.setText("🌤 Открыть прогноз погоды");
//        button.setWebApp(new WebAppInfo(webAppUrl));
//
//        buttons.add(List.of(button));
//        markup.setKeyboard(buttons);
//
//        SendMessage message = new SendMessage();
//        message.setChatId(chatId.toString());
//        message.setText("🌤 Прогноз погоды для вашей группы");
//        message.setReplyMarkup(markup);
//
//        messageSender.sendMessage(message);
//    }

    private Long parseUserId(String input) {
        if (input.startsWith("@")) {
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
}