package ru.polyrythms.telegrambot.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class ThreadPoolMetrics {

    private final ThreadPoolExecutor executor;
    private final MeterRegistry registry;

    public ThreadPoolMetrics(ThreadPoolExecutor executor, MeterRegistry registry) {
        this.executor = executor;
        this.registry = registry;

        registerMetrics();
    }

    private void registerMetrics() {
        // Размер очереди
        registry.gauge("telegram.threadpool.queue.size", executor,
                e -> e.getQueue().size());

        // Оставшаяся емкость очереди
        registry.gauge("telegram.threadpool.queue.remaining", executor,
                e -> e.getQueue().remainingCapacity());

        // Активные потоки
        registry.gauge("telegram.threadpool.active.threads", executor,
                ThreadPoolExecutor::getActiveCount);

        // Размер пула
        registry.gauge("telegram.threadpool.pool.size", executor,
                ThreadPoolExecutor::getPoolSize);

        // Завершенные задачи
        registry.gauge("telegram.threadpool.completed.tasks", executor,
                ThreadPoolExecutor::getCompletedTaskCount);

        // Всего задач
        registry.gauge("telegram.threadpool.total.tasks", executor,
                ThreadPoolExecutor::getTaskCount);
    }
}