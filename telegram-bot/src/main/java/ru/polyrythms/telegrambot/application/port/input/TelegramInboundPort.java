// src/main/java/ru/polyrythms/telegrambot/application/port/input/TelegramInboundPort.java

package ru.polyrythms.telegrambot.application.port.input;

import ru.polyrythms.telegrambot.application.dto.TelegramUpdateDto;

/**
 * Inbound Port для приема событий от Telegram.
 * Этот порт реализуется в application слое и вызывается из infrastructure слоя.
 * Он служит мостом между внешним миром (Telegram API) и бизнес-логикой приложения.
 */
public interface TelegramInboundPort {

    /**
     * Основной метод обработки любого обновления от Telegram.
     * Вызывается адаптером для каждого входящего update.
     *
     * @param update DTO с данными из Telegram
     */
    void handleUpdate(TelegramUpdateDto update);

    /**
     * Обработка голосового сообщения.
     * Вызывается из handleUpdate() после определения типа сообщения.
     *
     * @param voiceUpdate DTO с данными голосового сообщения
     */
    void handleVoiceMessage(TelegramUpdateDto voiceUpdate);

    /**
     * Обработка текстовой команды (начинается с /).
     * Вызывается из handleUpdate() после определения типа сообщения.
     *
     * @param commandUpdate DTO с данными команды
     */
    void handleCommand(TelegramUpdateDto commandUpdate);

    /**
     * Обработка обычного текстового сообщения (не команды).
     * Вызывается из handleUpdate() для текстовых сообщений без /.
     *
     * @param textUpdate DTO с текстовым сообщением
     */
    void handlePlainText(TelegramUpdateDto textUpdate);
}