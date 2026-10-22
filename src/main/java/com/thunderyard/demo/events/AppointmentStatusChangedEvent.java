package com.thunderyard.demo.events;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record AppointmentStatusChangedEvent(

        @NotNull UUID eventId,
        @NotNull UUID appointmentId,
        @NotNull UUID patientId,

        @NotNull Status fromStatus,
        @NotNull Status toStatus,

        Instant timestamp,
        String source
) {

    public enum Status {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        FOLLOW_UP_REQUIRED
    }
}
