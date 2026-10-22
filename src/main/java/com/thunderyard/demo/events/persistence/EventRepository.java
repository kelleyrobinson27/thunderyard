package com.thunderyard.demo.events.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {

    List<EventEntity> findByType(String type);

    List<EventEntity> findByCorrelationId(String correlationId);

    List<EventEntity> findByCreatedAtBetween(Instant start, Instant end);
}
