package com.thunderyard.demo.events;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record AppointmentCancelledEvent(

        @NotNull UUID eventId,
        @NotNull UUID appointmentId,
        @NotNull UUID patientId,

        @NotNull CancelReason reason,

        Instant timestamp,
        String source
) {

    public enum CancelReason {
        PATIENT_REQUEST,
        PROVIDER_REQUEST,
        SYSTEM
    }
}
