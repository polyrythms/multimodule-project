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
            .tag("command", "unknown")
            .register(meterRegistry);
        
        this.errorsOccurred = Counter.builder("telegram.errors")
            .description("Total errors")
            .register(meterRegistry);
        
        this.processingTime = Timer.builder("telegram.processing.time")
            .description("Message processing time")
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
    
    public Timer.Sample startTimer() {
        return Timer.start();
    }
    
    public void stopTimer(Timer.Sample sample) {
        sample.stop(processingTime);
    }
}