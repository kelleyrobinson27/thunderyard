package com.thunderyard.demo.events.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thunderyard.demo.config.CorrelationIds;
import org.springframework.stereotype.Component;

@Component
public class EventStore {

    private final EventRepository repository;
    private final ObjectMapper objectMapper;   // <-- injected

    public EventStore(EventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;      // <-- uses Spring Boot’s mapper
    }

    public void persist(Object event) {
        try {
            EventEntity entity = new EventEntity();
            entity.setType(event.getClass().getSimpleName());
            entity.setPayload(objectMapper.writeValueAsString(event));  // <-- now supports Instant
            entity.setSource("notification-service");
            entity.setCorrelationId(CorrelationIds.current());
            entity.setCreatedAt(java.time.Instant.now());
            entity.setReceivedAt(java.time.Instant.now());

            repository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist event", e);
        }
    }
}
