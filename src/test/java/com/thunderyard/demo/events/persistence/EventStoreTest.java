package com.thunderyard.demo.events.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.*;

class EventStoreTest {

    private final EventRepository repo = mock(EventRepository.class);
    private final ObjectMapper mapper = mock(ObjectMapper.class);   // <-- NEW

    private final EventStore store = new EventStore(repo, mapper);  // <-- UPDATED

    static class DummyEvent {
        public String name = "test-event";
        public int value = 42;
    }

    @Test
    void persistsEventAsJson() throws Exception {
        DummyEvent evt = new DummyEvent();

        // Mock JSON serialization
        when(mapper.writeValueAsString(evt))
                .thenReturn("{\"name\":\"test-event\",\"value\":42}");

        store.persist(evt);

        ArgumentCaptor<EventEntity> captor = ArgumentCaptor.forClass(EventEntity.class);
        verify(repo).save(captor.capture());

        EventEntity saved = captor.getValue();

        assert saved.getType().equals("DummyEvent");
        assert saved.getPayload().contains("\"name\":\"test-event\"");
        assert saved.getPayload().contains("\"value\":42");
        assert saved.getCreatedAt() != null;
        assert saved.getReceivedAt() != null;
    }
}
