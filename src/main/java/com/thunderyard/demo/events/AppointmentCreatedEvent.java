package com.thunderyard.demo.events;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record AppointmentCreatedEvent(

        @NotNull UUID eventId,
        @NotNull UUID appointmentId,
        @NotNull UUID patientId,

        Instant timestamp,
        String source
) {}
