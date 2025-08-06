package ru.polyrythms.telegram.bot;


import lombok.SneakyThrows;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Voice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.polyrythms.telegram.config.TelegramBotConfig;
import ru.polyrythms.telegram.service.AudioStorageService;

@Service
public class VoiceMessageBot extends TelegramLongPollingBot {

    private final TelegramBotConfig config;
    private final AudioStorageService audioStorageService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public VoiceMessageBot(TelegramBotConfig config, AudioStorageService audioStorageService,
                           KafkaTemplate<String, String> kafkaTemplate) {
        super(config.getBotToken());
        this.config = config;
        this.audioStorageService = audioStorageService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }


    @Override
    @SneakyThrows
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasVoice()) {
            Message message = update.getMessage();
            Voice voice = message.getVoice();
            Long chatId = message.getChatId();

            // Скачиваем голосовое сообщение
            GetFile getFile = new GetFile();
            getFile.setFileId(voice.getFileId());
            String filePath = execute(getFile).getFilePath();
            java.io.File voiceFile = downloadFile(filePath);

            // Сохраняем в MinIO
            String audioId = audioStorageService.storeAudio(voiceFile, voice.getFileUniqueId());

            // Отправляем задание в Kafka
            String task = String.format("{\"audioId\":\"%s\",\"chatId\":%d}", audioId, chatId);
            kafkaTemplate.send("audio-to-text-tasks", task);

            // Отправляем подтверждение пользователю
            sendTextMessage(chatId, "Ваше голосовое сообщение принято в обработку. Ожидайте результат...");
        }
    }

    private void sendTextMessage(Long chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        execute(message);
    }
}