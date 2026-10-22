package com.thunderyard.demo.events.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thunderyard.demo.events.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class EventReplayService {

    // === Added: whitelist of valid event types ===
    private static final Set<String> VALID_TYPES = Set.of(
            "AppointmentChangedEvent",
            "AppointmentReminderEvent",
            "AppointmentCreatedEvent",
            "AppointmentCancelledEvent",
            "AppointmentStatusChangedEvent",
            "AppointmentConfirmationEvent"
    );

    private final EventRepository repository;
    private final AppointmentEventListener listener;
    private final ObjectMapper mapper;

    public EventReplayService(EventRepository repository,
                              AppointmentEventListener listener,
                              ObjectMapper mapper) {
        this.repository = repository;
        this.listener = listener;
        this.mapper = mapper;
    }

    public int replayAll() {
        List<EventEntity> events = repository.findAll();
        return replay(events);
    }

    public int replayByType(String type) {

        // === Added: validate event type before replay ===
        if (!VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unknown event type: " + type);
        }

        List<EventEntity> events = repository.findByType(type);
        return replay(events);
    }

    private int replay(List<EventEntity> events) {

        if (events.size() > ReplayTooLargeException.MAX_EVENTS) {
            throw new ReplayTooLargeException(events.size());
        }

        int count = 0;

        for (EventEntity entity : events) {
            Object event = deserialize(entity);
            dispatch(event);
            count++;
        }

        return count;
    }

    private Object deserialize(EventEntity entity) {
        try {
            // === NEW: guard against missing or empty payload ===
            if (entity.getPayload() == null || entity.getPayload().isBlank()) {
                throw new IllegalArgumentException("Event " + entity.getId() + " has no payload.");
            }

            return switch (entity.getType()) {
                case "AppointmentChangedEvent" ->
                        mapper.readValue(entity.getPayload(), AppointmentChangedEvent.class);
                case "AppointmentCreatedEvent" ->
                        mapper.readValue(entity.getPayload(), AppointmentCreatedEvent.class);
                case "AppointmentCancelledEvent" ->
                        mapper.readValue(entity.getPayload(), AppointmentCancelledEvent.class);
                case "AppointmentReminderEvent" ->
                        mapper.readValue(entity.getPayload(), AppointmentReminderEvent.class);
                case "AppointmentStatusChangedEvent" ->
                        mapper.readValue(entity.getPayload(), AppointmentStatusChangedEvent.class);
                case "AppointmentConfirmationEvent" ->
                        mapper.readValue(entity.getPayload(), AppointmentConfirmationEvent.class);
                default -> throw new IllegalArgumentException("Unknown event type: " + entity.getType());
            };
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event: " + entity.getId(), e);
        }
    }

    private void dispatch(Object event) {
        if (event instanceof AppointmentChangedEvent e) listener.handleEvent(e);
        else if (event instanceof AppointmentCreatedEvent e) listener.handleEvent(e);
        else if (event instanceof AppointmentCancelledEvent e) listener.handleEvent(e);
        else if (event instanceof AppointmentReminderEvent e) listener.handleEvent(e);
        else if (event instanceof AppointmentStatusChangedEvent e) listener.handleEvent(e);
        else if (event instanceof AppointmentConfirmationEvent e) listener.handleEvent(e);
        else throw new IllegalArgumentException("Unhandled event type: " + event.getClass());
    }
}
