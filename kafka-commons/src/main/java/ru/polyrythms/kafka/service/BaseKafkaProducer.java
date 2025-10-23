package ru.polyrythms.kafka.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class BaseKafkaProducer<T> {

    private final KafkaTemplate<String, T> kafkaTemplate;
    private final String topic;

    protected BaseKafkaProducer(KafkaTemplate<String, T> kafkaTemplate, String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public Mono<SendResult<String, T>> sendMessage(String key, T message) {
        return Mono.create(sink -> kafkaTemplate.send(topic, key, message)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to send message to topic: {}, key: {}", topic, key, error);
                        sink.error(error);
                    } else {
                        log.debug("Message sent successfully to topic: {}, key: {}, offset: {}",
                                topic, key, result.getRecordMetadata().offset());
                        sink.success(result);
                    }
                }));
    }


    public Mono<Void> send(String key, T message) {
        return sendMessage(key, message)
                .then()
                .doOnSuccess(v -> log.debug("Message sent to topic: {}, key: {}", topic, key))
                .doOnError(error -> log.error("Failed to send message to topic: {}, key: {}", topic, key, error));
    }

    protected String getTopic() {
        return topic;
    }
}