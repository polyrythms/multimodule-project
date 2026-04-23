package ru.polyrythms.telegrambot.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import ru.polyrythms.telegrambot.infrastructure.adapter.output.telegram.TelegramBotClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMembershipService {

    private final TelegramBotClient botClient;

    /**
     * Проверяет, является ли пользователь участником группы
     * @param chatId ID группы
     * @param userId ID пользователя
     * @return true если пользователь состоит в группе, false в противном случае
     */
    @Cacheable(value = "membership", key = "#chatId + ':' + #userId", unless = "#result == false")
    public boolean isUserMemberOfGroup(Long chatId, Long userId) {
        try {
            GetChatMember getChatMember = new GetChatMember();
            getChatMember.setChatId(chatId.toString());
            getChatMember.setUserId(userId);

            ChatMember chatMember = botClient.execute(getChatMember);
            String status = chatMember.getStatus();

            boolean isMember = "member".equals(status)
                    || "administrator".equals(status)
                    || "creator".equals(status);

            log.debug("User {} membership in group {}: {} (status: {})", userId, chatId, isMember, status);
            return isMember;

        } catch (RuntimeException e) {
            if (e.getMessage().contains("user not found") || e.getMessage().contains("USER_ID_INVALID")) {
                log.warn("User {} is not a member of group {}", userId, chatId);
                return false;
            }
            log.error("Failed to check membership for user {} in group {}", userId, chatId, e);
            return false;
        }
    }
}