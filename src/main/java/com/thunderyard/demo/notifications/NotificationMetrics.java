package com.thunderyard.demo.notifications;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics implements MeterBinder {

    private Counter queued;
    private Counter sent;
    private Counter delivered;
    private Counter failed;
    private Counter retried;
    private Counter duplicated;
    private Counter suppressed;

    @Override
    public void bindTo(MeterRegistry registry) {
        this.queued = Counter.builder("notifications.queued")
                .description("Notifications queued for SMS").register(registry);
        this.sent = Counter.builder("notifications.sent")
                .description("SMS accepted by provider").register(registry);
        this.delivered = Counter.builder("notifications.delivered")
                .description("Delivery callbacks with DELIVERED").register(registry);
        this.failed = Counter.builder("notifications.failed")
                .description("Permanent send or delivery failures").register(registry);
        this.retried = Counter.builder("notifications.retried")
                .description("Jobs requeued after retryable SMS failure").register(registry);
        this.duplicated = Counter.builder("notifications.duplicated")
                .description("Events skipped because eventId already exists").register(registry);
        this.suppressed = Counter.builder("notifications.suppressed")
                .description("Events suppressed (cancel or canNotify=false)").register(registry);
    }

    public void queued() {
        queued.increment();
    }

    public void sent() {
        sent.increment();
    }

    public void delivered() {
        delivered.increment();
    }

    public void failed() {
        failed.increment();
    }

    public void retried() {
        retried.increment();
    }

    public void duplicated() {
        duplicated.increment();
    }

    public void suppressed() {
        suppressed.increment();
    }
}
