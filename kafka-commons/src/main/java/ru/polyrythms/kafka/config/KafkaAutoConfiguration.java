package ru.polyrythms.kafka.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import ru.polyrythms.kafka.Topics;

import java.util.HashMap;
import java.util.Map;

/**
 * Автоконфигурация Kafka топиков для микросервисов Polyrythms.
 *
 * <p>Данный класс автоматически настраивает и создает необходимые Kafka топики
 * при наличии зависимости {@code kafka-commons} в classpath и выполнении условий.</p>
 *
 * <h2>Механизм активации:</h2>
 * <ul>
 *   <li>Автоматически активируется через файл автоконфигурации Spring Boot</li>
 *   <li>Файл конфигурации: {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}</li>
 *   <li>Требует наличия Spring Kafka в classpath</li>
 * </ul>
 *
 * <h2>Создаваемые топики:</h2>
 * <ul>
 *   <li>{@value ru.polyrythms.kafka.Topics#AUDIO_DECRYPTION_REQUESTS} - для запросов на расшифровку аудио</li>
 *   <li>{@value ru.polyrythms.kafka.Topics#AUDIO_DECRYPTION_RESULTS} - для результатов расшифровки</li>
 * </ul>
 *
 * <h2>Условия активации:</h2>
 * <ul>
 *   <li>Наличие Spring Kafka в classpath</li>
 *   <li>Свойство {@code app.kafka.topics.auto-create} равно {@code true} (значение по умолчанию)</li>
 *   <li>Отсутствие ручной конфигурации топиков в основном приложении</li>
 * </ul>
 *
 * <h2>Настройки через properties:</h2>
 * <pre>{@code
 * app:
 *   kafka:
 *     bootstrap-servers: localhost:9092
 *     topics:
 *       partitions: 6
 *       replicas: 1
 *       retention-ms: 604800000
 *       auto-create: true
 * }</pre>
 *
 * <p><b>Примечание:</b> При запуске нескольких микросервисов с данной конфигурацией
 * Kafka безопасно обрабатывает попытки повторного создания топиков, игнорируя их
 * если они уже существуют с совместимыми параметрами.</p>
 *
 * @author Polyrythms Development Team
 * @version 1.0
 * @see org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * @see KafkaTopicsProperties
 * @since 2024
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.kafka.core.KafkaTemplate")
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class KafkaAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(KafkaAutoConfiguration.class);

    private final KafkaTopicsProperties properties;

    /**
     * Конструктор автоконфигурации.
     *
     * @param properties настройки Kafka топиков, загружаемые из application.properties/yaml
     */
    public KafkaAutoConfiguration(KafkaTopicsProperties properties) {
        this.properties = properties;
        log.debug("KafkaAutoConfiguration инициализирован с настройками: {}", properties);
    }

    /**
     * Создает и настраивает бин {@link KafkaAdmin} для управления топиками Kafka.
     *
     * <p>Бин создается только если:</p>
     * <ul>
     *   <li>Отсутствует ручно настроенный бин KafkaAdmin</li>
     *   <li>Свойство {@code app.kafka.admin.enabled} не равно {@code false}</li>
     * </ul>
     *
     * @return настроенный экземпляр KafkaAdmin
     * @see org.springframework.kafka.core.KafkaAdmin
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "app.kafka.admin.enabled", havingValue = "true", matchIfMissing = true)
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                properties.getBootstrapServers());

        log.info("Создание KafkaAdmin с bootstrap servers: {}",
                properties.getBootstrapServers());
        return new KafkaAdmin(configs);
    }

    /**
     * Создает топик для запросов на расшифровку аудио сообщений.
     *
     * <p>Топик используется для отправки заданий на расшифровку от Telegram бота
     * к сервису обработки аудио.</p>
     *
     * <p>Бин создается только если:</p>
     * <ul>
     *   <li>Отсутствует бин с именем {@code audioDecryptionRequestsTopic}</li>
     *   <li>Свойство {@code app.kafka.topics.auto-create} не равно {@code false}</li>
     * </ul>
     *
     * @return конфигурация топика запросов на расшифровку аудио
     * @see org.apache.kafka.clients.admin.NewTopic
     */
    @Bean
    @ConditionalOnMissingBean(name = "audioDecryptionRequestsTopic")
    @ConditionalOnProperty(name = "app.kafka.topics.auto-create", havingValue = "true", matchIfMissing = true)
    public NewTopic audioDecryptionRequestsTopic() {
        log.info("Автосоздание Kafka топика: {} с {} партициями и {} репликами",
                Topics.AUDIO_DECRYPTION_REQUESTS,
                properties.getPartitions(),
                properties.getReplicas());

        return TopicBuilder.name(Topics.AUDIO_DECRYPTION_REQUESTS)
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicas())
                .config("retention.ms", String.valueOf(properties.getRetentionMs()))
                .config("cleanup.policy", "delete")
                .build();
    }

    /**
     * Создает топик для результатов расшифровки аудио сообщений.
     *
     * <p>Топик используется для отправки результатов расшифровки от сервиса обработки аудио
     * обратно к Telegram боту.</p>
     *
     * <p>Бин создается только если:</p>
     * <ul>
     *   <li>Отсутствует бин с именем {@code audioDecryptionResultsTopic}</li>
     *   <li>Свойство {@code app.kafka.topics.auto-create} не равно {@code false}</li>
     * </ul>
     *
     * @return конфигурация топика результатов расшифровки аудио
     * @see org.apache.kafka.clients.admin.NewTopic
     */
    @Bean
    @ConditionalOnMissingBean(name = "audioDecryptionResultsTopic")
    @ConditionalOnProperty(name = "app.kafka.topics.auto-create", havingValue = "true", matchIfMissing = true)
    public NewTopic audioDecryptionResultsTopic() {
        log.info("Автосоздание Kafka топика: {} с {} партициями и {} репликами",
                Topics.AUDIO_DECRYPTION_RESULTS,
                properties.getPartitions(),
                properties.getReplicas());

        return TopicBuilder.name(Topics.AUDIO_DECRYPTION_RESULTS)
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicas())
                .config("retention.ms", String.valueOf(properties.getRetentionMs()))
                .config("cleanup.policy", "delete")
                .build();
    }
}