package com.thunderyard.demo.events;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record AppointmentConfirmationEvent(

        @NotNull UUID eventId,
        @NotNull UUID appointmentId,
        @NotNull UUID patientId,

        @NotNull ConfirmationType confirmationType,

        Instant timestamp,
        String source
) {

    public enum ConfirmationType {
        PATIENT_CONFIRMED,
        PROVIDER_CONFIRMED
    }
}
