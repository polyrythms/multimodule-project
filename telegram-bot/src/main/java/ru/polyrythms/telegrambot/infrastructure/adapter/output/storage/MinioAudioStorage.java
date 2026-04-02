package ru.polyrythms.telegrambot.infrastructure.adapter.output.storage;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.polyrythms.telegrambot.application.port.output.AudioStorage;

import java.io.File;
import java.io.FileInputStream;

@Component
@RequiredArgsConstructor
public class MinioAudioStorage implements AudioStorage {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${minio.url}")
    private String minioUrl;

    @Override
    @SneakyThrows
    public String storeAudio(File audioFile, String uniqueId) {
        try (FileInputStream stream = new FileInputStream(audioFile)) {
            String objectName = "audio-" + uniqueId + ".ogg";
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(stream, audioFile.length(), -1)
                            .contentType("audio/ogg")
                            .build()
            );
            return objectName;
        }
    }

    @Override
    public String getPublicUrl(String objectName) {
        return String.format("%s/%s/%s", minioUrl, bucketName, objectName);
    }
}