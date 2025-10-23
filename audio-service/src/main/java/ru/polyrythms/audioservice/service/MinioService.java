package ru.polyrythms.audioservice.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;

@Service
public class MinioService {

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioService(MinioClient minioClient,
                        @Value("${minio.bucket}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    public Mono<byte[]> downloadAudio(String audioId) {
        return Mono.fromCallable(() -> {
                    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                        minioClient.getObject(GetObjectArgs.builder()
                                        .bucket(bucketName)
                                        .object(audioId)
                                        .build())
                                .transferTo(outputStream);
                        return outputStream.toByteArray();
                    }
                })
                .subscribeOn(Schedulers.boundedElastic()); // Блокирующая операция в отдельном потоке
    }
}