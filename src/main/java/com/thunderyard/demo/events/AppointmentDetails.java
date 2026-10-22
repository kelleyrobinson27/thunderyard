package com.thunderyard.demo.events;

import java.util.UUID;

/**
 * Appointment details fetched from EAS.
 */
public record AppointmentDetails(
        UUID appointmentId,
        String providerName,
        String appointmentDate,
        boolean canNotify
) {}
