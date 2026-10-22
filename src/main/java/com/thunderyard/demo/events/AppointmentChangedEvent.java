package com.thunderyard.demo.events;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing a change to an appointment.
 *
 * This record is used as the payload for Kafka events and drives
 * the notification pipeline.
 */
public record AppointmentChangedEvent(

        @NotNull UUID eventId,
        @NotNull UUID appointmentId,
        @NotNull UUID patientId,
        @NotNull EventType eventType,

        // Optional metadata fields
        Instant timestamp,
        String source
) {

    /**
     * Type of appointment change.
     */
    public enum EventType {
        CREATED,
        UPDATED,
        CANCELLED
    }
}
