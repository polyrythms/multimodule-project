package ru.polyrythms.telegrambot.infrastructure.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Component
public class ThreadPoolMonitor {

    private final ThreadPoolExecutor inboundExecutor;
    private final ThreadPoolExecutor outboundExecutor;

    public ThreadPoolMonitor(
            @Qualifier("telegramInboundExecutor") ThreadPoolExecutor inboundExecutor,
            @Qualifier("telegramOutboundExecutor") ThreadPoolExecutor outboundExecutor) {
        this.inboundExecutor = inboundExecutor;
        this.outboundExecutor = outboundExecutor;
    }

    /**
     * Проверка состояния пулов каждые 30 секунд
     */
    @Scheduled(fixedDelay = 30000)
    public void monitorThreadPools() {
        checkInboundPool();
        checkOutboundPool();
    }

    private void checkInboundPool() {
        int queueSize = inboundExecutor.getQueue().size();
        int queueCapacity = inboundExecutor.getQueue().remainingCapacity() + queueSize;
        int activeThreads = inboundExecutor.getActiveCount();
        int poolSize = inboundExecutor.getPoolSize();

        // Предупреждение при заполнении очереди > 80%
        if (queueSize > queueCapacity * 0.8) {
            log.warn("INBOUND thread pool queue is almost full! {}/{} tasks waiting",
                    queueSize, queueCapacity);
        }

        // Предупреждение при высокой загрузке потоков > 90%
        double utilization = activeThreads / (double) poolSize;
        if (utilization > 0.9) {
            String message = String.format("INBOUND thread pool utilization is high: %.1f%% (%d/%d)",
                    utilization * 100, activeThreads, poolSize);
            log.warn(message);
        }

        // Логируем при нормальной работе (для отладки)
        if (log.isDebugEnabled()) {
            log.debug("INBOUND pool status: queue={}/{}, active={}/{}, completed={}",
                    queueSize, queueCapacity, activeThreads, poolSize,
                    inboundExecutor.getCompletedTaskCount());
        }
    }

    private void checkOutboundPool() {
        int queueSize = outboundExecutor.getQueue().size();
        int queueCapacity = outboundExecutor.getQueue().remainingCapacity() + queueSize;
        int activeThreads = outboundExecutor.getActiveCount();
        int poolSize = outboundExecutor.getPoolSize();

        // OUTBOUND пул обычно менее критичен, но тоже мониторим
        if (queueSize > queueCapacity * 0.9) {
            log.warn("OUTBOUND thread pool queue is full! {}/{} tasks waiting",
                    queueSize, queueCapacity);
        }

        if (log.isDebugEnabled()) {
            log.debug("OUTBOUND pool status: queue={}/{}, active={}/{}, completed={}",
                    queueSize, queueCapacity, activeThreads, poolSize,
                    outboundExecutor.getCompletedTaskCount());
        }
    }
}