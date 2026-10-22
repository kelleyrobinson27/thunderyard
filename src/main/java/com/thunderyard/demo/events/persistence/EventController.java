package com.thunderyard.demo.events.persistence;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventRepository repository;

    public EventController(EventRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<EventEntity> getEvents(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String correlationId
    ) {
        if (correlationId != null) {
            return repository.findByCorrelationId(correlationId);
        }

        // Filter by type only
        if (type != null && from == null && to == null) {
            return repository.findByType(type);
        }

        // Filter by date range
        if (from != null && to != null) {
            Instant start = Instant.parse(from);
            Instant end = Instant.parse(to);
            return repository.findByCreatedAtBetween(start, end);
        }

        // Default: return all events
        return repository.findAll();
    }

    @GetMapping("/count")
    public long getCount(@RequestParam(required = false) String type) {
        if (type != null) {
            return repository.countByType(type);
        }
        return repository.count();
    }
}
