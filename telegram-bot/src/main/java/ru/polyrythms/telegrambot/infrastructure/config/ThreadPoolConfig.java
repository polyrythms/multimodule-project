package ru.polyrythms.telegrambot.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
public class ThreadPoolConfig {

    @Value("${telegram.thread-pool.inbound.queue-capacity:50}")
    private int inboundQueueCapacity;

    @Value("${telegram.thread-pool.inbound.core-size:0}")
    private int inboundConfiguredCoreSize;

    @Value("${telegram.thread-pool.outbound.queue-capacity:100}")
    private int outboundQueueCapacity;

    @Value("${telegram.thread-pool.outbound.core-size:0}")
    private int outboundConfiguredCoreSize;

    @Bean("telegramInboundExecutor")
    public ThreadPoolExecutor telegramInboundExecutor(MeterRegistry meterRegistry) {
        int cores = Runtime.getRuntime().availableProcessors();
        int poolSize = inboundConfiguredCoreSize > 0 ? inboundConfiguredCoreSize : cores;

        log.info("Initializing INBOUND thread pool with {} threads (cores: {}), queue capacity: {}",
                poolSize, cores, inboundQueueCapacity);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(inboundQueueCapacity);
        executor.setThreadNamePrefix("telegram-inbound-");
        executor.setDaemon(false);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new UserFriendlyRejectionHandler(meterRegistry));
        executor.initialize();

        ThreadPoolExecutor pool = executor.getThreadPoolExecutor();
        registerThreadPoolMetrics(pool, meterRegistry, "inbound");

        log.info("INBOUND thread pool initialized successfully");
        return pool;
    }

    @Bean("telegramOutboundExecutor")
    public ThreadPoolExecutor telegramOutboundExecutor(MeterRegistry meterRegistry) {
        int cores = Runtime.getRuntime().availableProcessors();
        int poolSize = outboundConfiguredCoreSize > 0 ? outboundConfiguredCoreSize : cores;

        log.info("Initializing OUTBOUND thread pool with {} threads (cores: {}), queue capacity: {}",
                poolSize, cores, outboundQueueCapacity);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(outboundQueueCapacity);
        executor.setThreadNamePrefix("telegram-outbound-");
        executor.setDaemon(false);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        ThreadPoolExecutor pool = executor.getThreadPoolExecutor();
        registerThreadPoolMetrics(pool, meterRegistry, "outbound");

        log.info("OUTBOUND thread pool initialized successfully");
        return pool;
    }

    /**
     * Универсальный метод для регистрации метрик пула потоков
     *
     * @param executor      пул потоков
     * @param meterRegistry реестр метрик Micrometer
     * @param poolName      имя пула (inbound/outbound)
     */
    private void registerThreadPoolMetrics(ThreadPoolExecutor executor,
                                           MeterRegistry meterRegistry,
                                           String poolName) {
        String prefix = "telegram.threadpool." + poolName;

        // Размер очереди
        meterRegistry.gauge(prefix + ".queue.size", executor,
                e -> e.getQueue().size());

        // Оставшаяся емкость очереди
        meterRegistry.gauge(prefix + ".queue.remaining", executor,
                e -> e.getQueue().remainingCapacity());

        // Активные потоки
        meterRegistry.gauge(prefix + ".active.threads", executor,
                ThreadPoolExecutor::getActiveCount);

        // Текущий размер пула
        meterRegistry.gauge(prefix + ".pool.size", executor,
                ThreadPoolExecutor::getPoolSize);

        // Максимальный размер пула
        meterRegistry.gauge(prefix + ".max.pool.size", executor,
                ThreadPoolExecutor::getMaximumPoolSize);

        // Количество завершенных задач
        meterRegistry.gauge(prefix + ".completed.tasks", executor,
                ThreadPoolExecutor::getCompletedTaskCount);

        // Общее количество задач (включая активные и завершенные)
        meterRegistry.gauge(prefix + ".total.tasks", executor,
                ThreadPoolExecutor::getTaskCount);

        // Количество задач в очереди (для Prometheus)
        meterRegistry.gauge(prefix + ".queue.waiting", executor,
                e -> e.getQueue().size());

        // Процент загрузки активных потоков
        meterRegistry.gauge(prefix + ".utilization", executor,
                e -> e.getActiveCount() / (double) e.getPoolSize());

        log.debug("Registered metrics for {} thread pool: {}.queue.size, {}.active.threads, etc.",
                poolName, prefix, prefix);
    }
}