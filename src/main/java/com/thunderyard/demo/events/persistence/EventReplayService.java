package com.thunderyard.demo.events.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thunderyard.demo.events.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventReplayService {

    private final EventRepository repository;
    private final AppointmentEventListener listener;
    private final ObjectMapper mapper;   // <-- injected

    public EventReplayService(EventRepository repository,
                              AppointmentEventListener listener,
                              ObjectMapper mapper) {
        this.repository = repository;
        this.listener = listener;
        this.mapper = mapper;            // <-- uses Spring Boot’s mapper
    }

    public int replayAll() {
        List<EventEntity> events = repository.findAll();
        return replay(events);
    }

    public int replayByType(String type) {
        List<EventEntity> events = repository.findByType(type);
        return replay(events);
    }

    private int replay(List<EventEntity> events) {

        if (events.size() > 50) {
            throw new IllegalStateException("Replay too large — aborting.");
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
