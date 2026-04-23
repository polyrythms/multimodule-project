package ru.polyrythms.telegrambot.application.port.output;

import ru.polyrythms.telegrambot.domain.model.GroupCity;
import java.util.List;

public interface GroupCityRepository {
    List<GroupCity> findByGroupChatId(Long groupChatId);
    void assignCityToGroup(Long groupChatId, Long cityId);
    void removeCityFromGroup(Long groupChatId, Long cityId);
}