package ru.polyrythms.telegrambot.infrastructure.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import ru.polyrythms.telegrambot.infrastructure.task.VoiceMessageTask;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class UserFriendlyRejectionHandler implements RejectedExecutionHandler {

    private final Counter rejectionCounter;
    private final AtomicLong lastLogTime = new AtomicLong(0);
    private final AtomicLong lastNotificationTime = new AtomicLong(0);

    // Cooldown для уведомлений (чтобы не спамить)
    private static final long NOTIFICATION_COOLDOWN_MS = 5000; // 5 секунд
    private static final long LOG_COOLDOWN_MS = 10000; // 10 секунд

    public UserFriendlyRejectionHandler(MeterRegistry meterRegistry) {
        this.rejectionCounter = Counter.builder("telegram.tasks.rejected")
                .description("Number of rejected tasks due to queue overflow")
                .register(meterRegistry);
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        // Инкрементируем метрику
        rejectionCounter.increment();

        // Логируем состояние пула (не чаще раза в 10 секунд)
        logPoolState(executor);

        // Пытаемся уведомить пользователя
        notifyUserIfPossible(r);

        // Задача отбрасывается (не выполняется)
        log.debug("Task rejected and discarded");
    }

    private void logPoolState(ThreadPoolExecutor executor) {
        long now = System.currentTimeMillis();
        if (now - lastLogTime.get() > LOG_COOLDOWN_MS) {
            lastLogTime.set(now);

            int queueSize = executor.getQueue().size();
            int activeCount = executor.getActiveCount();
            int poolSize = executor.getPoolSize();
            long completedCount = executor.getCompletedTaskCount();

            log.warn("Thread pool overload! " +
                            "Queue size: {}/{}, " +
                            "Active threads: {}/{}, " +
                            "Completed tasks: {}",
                    queueSize, executor.getQueue().remainingCapacity() + queueSize,
                    activeCount, poolSize,
                    completedCount);
        }
    }

    private void notifyUserIfPossible(Runnable r) {
        long now = System.currentTimeMillis();

        // Ограничиваем частоту уведомлений
        if (now - lastNotificationTime.get() < NOTIFICATION_COOLDOWN_MS) {
            return;
        }

        lastNotificationTime.set(now);

        if (r instanceof VoiceMessageTask task) {
            // Асинхронно отправляем уведомление, не блокируя текущий поток
            CompletableFuture.runAsync(() -> {
                try {
                    task.sendOverloadNotification();
                    log.debug("Sent overload notification to chatId: {}", task.getChatId());
                } catch (Exception e) {
                    log.error("Failed to send overload notification", e);
                }
            });
        } else {
            log.debug("Cannot notify user for task type: {}", r.getClass().getSimpleName());
        }
    }
}