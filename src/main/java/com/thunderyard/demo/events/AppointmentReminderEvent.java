package com.thunderyard.demo.events;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event representing a reminder for an upcoming appointment.
 */
public record AppointmentReminderEvent(

        @NotNull UUID eventId,
        @NotNull UUID appointmentId,
        @NotNull UUID patientId,
        @NotNull ReminderType reminderType,

        Instant timestamp,
        String source
) {

    public enum ReminderType {
        UPCOMING,
        DAY_OF,
        LATE
    }
}
