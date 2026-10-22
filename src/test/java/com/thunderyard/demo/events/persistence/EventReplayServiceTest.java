package com.thunderyard.demo.events.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thunderyard.demo.config.JacksonConfig;
import com.thunderyard.demo.events.AppointmentChangedEvent;
import com.thunderyard.demo.events.AppointmentEventListener;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventReplayServiceTest {

    private final EventRepository repo = mock(EventRepository.class);
    private final AppointmentEventListener listener = mock(AppointmentEventListener.class);
    private final ObjectMapper mapper = new JacksonConfig().objectMapper();
    private final EventReplayService service = new EventReplayService(repo, listener, mapper);

    @Test
    void rejectsUnknownType() {
        assertThatThrownBy(() -> service.replayByType("TotallyFakeEventType"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown event type");
    }

    @Test
    void abortsWhenMoreThanFiftyEvents() {
        List<EventEntity> tooMany = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            tooMany.add(new EventEntity());
        }
        when(repo.findByType("AppointmentChangedEvent")).thenReturn(tooMany);

        assertThatThrownBy(() -> service.replayByType("AppointmentChangedEvent"))
                .isInstanceOf(ReplayTooLargeException.class)
                .hasMessageContaining("limited to 50")
                .hasMessageContaining("got 51");
    }

    @Test
    void rejectsBlankPayload() {
        EventEntity entity = new EventEntity();
        entity.setId(UUID.randomUUID());
        entity.setType("AppointmentChangedEvent");
        entity.setPayload("  ");
        when(repo.findByType("AppointmentChangedEvent")).thenReturn(List.of(entity));

        assertThatThrownBy(() -> service.replayByType("AppointmentChangedEvent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to deserialize");
    }

    @Test
    void deserializesAndDispatchesChangedEvent() throws Exception {
        AppointmentChangedEvent event = new AppointmentChangedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                AppointmentChangedEvent.EventType.UPDATED,
                Instant.parse("2026-10-21T17:00:00Z"),
                "unit-test"
        );
        EventEntity entity = new EventEntity();
        entity.setId(UUID.randomUUID());
        entity.setType("AppointmentChangedEvent");
        entity.setPayload(mapper.writeValueAsString(event));
        when(repo.findAll()).thenReturn(List.of(entity));

        int count = service.replayAll();

        assertThat(count).isEqualTo(1);
        verify(listener).handleEvent(event);
    }
}
