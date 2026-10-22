package com.thunderyard.demo.events.persistence;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class EventControllerTest {

    private final EventRepository repo = mock(EventRepository.class);
    private final EventController controller = new EventController(repo);

    @Test
    void returnsAllEvents() {
        EventEntity e = new EventEntity();
        e.setId(UUID.randomUUID());
        e.setType("TestEvent");
        e.setCreatedAt(Instant.now());
        e.setReceivedAt(Instant.now());

        when(repo.findAll()).thenReturn(List.of(e));

        List<EventEntity> result = controller.getEvents(null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo("TestEvent");
    }

    @Test
    void filtersByType() {
        when(repo.findByType("FooEvent")).thenReturn(List.of(new EventEntity()));

        List<EventEntity> result = controller.getEvents("FooEvent", null, null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void filtersByDateRange() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        Instant end = Instant.parse("2024-02-01T00:00:00Z");

        when(repo.findByCreatedAtBetween(start, end))
                .thenReturn(List.of(new EventEntity()));

        List<EventEntity> result = controller.getEvents(null, start.toString(), end.toString(), null);

        assertThat(result).hasSize(1);
    }

    @Test
    void filtersByCorrelationId() {
        when(repo.findByCorrelationId("corr-demo")).thenReturn(List.of(new EventEntity()));

        List<EventEntity> result = controller.getEvents(null, null, null, "corr-demo");

        assertThat(result).hasSize(1);
        verify(repo).findByCorrelationId("corr-demo");
    }
}
