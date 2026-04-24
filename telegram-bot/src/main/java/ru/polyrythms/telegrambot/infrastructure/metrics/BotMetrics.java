package ru.polyrythms.telegrambot.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class BotMetrics {

    private final Counter messagesReceived;
    private final Counter voiceMessagesReceived;
    private final Counter commandsReceived;
    private final Counter errorsOccurred;
    private final Counter tasksSubmitted;
    private final Counter tasksRejected;
    private final Counter authInitAttempt;
    private final Counter authInitSuccess;
    private final Counter authInitFailure;
    private final Timer processingTime;

    public BotMetrics(MeterRegistry meterRegistry) {
        this.messagesReceived = Counter.builder("telegram.messages.received")
                .description("Total messages received")
                .register(meterRegistry);

        this.voiceMessagesReceived = Counter.builder("telegram.voice.received")
                .description("Total voice messages received")
                .register(meterRegistry);

        this.commandsReceived = Counter.builder("telegram.commands.received")
                .description("Total commands received")
                .register(meterRegistry);

        this.errorsOccurred = Counter.builder("telegram.errors")
                .description("Total errors")
                .register(meterRegistry);

        this.tasksSubmitted = Counter.builder("telegram.tasks.submitted")
                .description("Total tasks submitted to thread pool")
                .register(meterRegistry);

        this.tasksRejected = Counter.builder("telegram.tasks.rejected")
                .description("Total tasks rejected by thread pool")
                .register(meterRegistry);

        this.processingTime = Timer.builder("telegram.processing.time")
                .description("Message processing time")
                .register(meterRegistry);

        this.authInitAttempt = Counter.builder("telegram.auth.init.attempt")
                .description("Number of /auth/init attempts")
                .register(meterRegistry);

        this.authInitSuccess = Counter.builder("telegram.auth.init.success")
                .description("Number of successful /auth/init")
                .register(meterRegistry);

        this.authInitFailure = Counter.builder("telegram.auth.init.failure")
                .description("Number of failed /auth/init")
                .register(meterRegistry);
    }

    public void recordMessage() {
        messagesReceived.increment();
    }

    public void recordVoiceMessage() {
        voiceMessagesReceived.increment();
    }

    public void recordCommand(String command) {
        commandsReceived.increment();
    }

    public void recordError() {
        errorsOccurred.increment();
    }

    public void recordTaskSubmitted() {
        tasksSubmitted.increment();
    }

    public void recordTaskRejected() {
        tasksRejected.increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void stopTimer(Timer.Sample sample) {
        sample.stop(processingTime);
    }

    public void recordAuthInitAttempt() { authInitAttempt.increment(); }

    public void recordAuthInitSuccess() { authInitSuccess.increment(); }

    public void recordAuthInitFailure() { authInitFailure.increment(); }
}