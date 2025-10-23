package ru.polyrythms.telegrambot.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;

@Service
public class AudioStorageService {

    private final MinioClient minioClient;
    @Value("${minio.bucket}")
    private String bucketName;
    @Value("${minio.url}")
    private String minioUrl;

    public AudioStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

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

    public String getPublicAudioUrl(String objectName) {
        return String.format("%s/%s/%s",
                minioUrl,
                bucketName,
                objectName);
    }
}