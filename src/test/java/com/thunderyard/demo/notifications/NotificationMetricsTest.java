package com.thunderyard.demo.notifications;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMetricsTest {

    @Test
    void incrementsRegisteredCounters() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        NotificationMetrics metrics = new NotificationMetrics();
        metrics.bindTo(registry);

        metrics.queued();
        metrics.sent();
        metrics.delivered();
        metrics.failed();
        metrics.retried();
        metrics.duplicated();
        metrics.suppressed();

        assertThat(registry.counter("notifications.queued").count()).isEqualTo(1.0);
        assertThat(registry.counter("notifications.sent").count()).isEqualTo(1.0);
        assertThat(registry.counter("notifications.delivered").count()).isEqualTo(1.0);
        assertThat(registry.counter("notifications.failed").count()).isEqualTo(1.0);
        assertThat(registry.counter("notifications.retried").count()).isEqualTo(1.0);
        assertThat(registry.counter("notifications.duplicated").count()).isEqualTo(1.0);
        assertThat(registry.counter("notifications.suppressed").count()).isEqualTo(1.0);
    }
}
