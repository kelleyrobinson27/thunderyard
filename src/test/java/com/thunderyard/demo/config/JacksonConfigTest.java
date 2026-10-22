package com.thunderyard.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thunderyard.demo.events.AppointmentChangedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    @Test
    void roundTripsAppointmentChangedEventWithInstant() throws Exception {
        ObjectMapper mapper = new JacksonConfig().objectMapper();
        AppointmentChangedEvent event = new AppointmentChangedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                AppointmentChangedEvent.EventType.CREATED,
                Instant.parse("2026-10-21T17:00:00Z"),
                "jackson");

        AppointmentChangedEvent read = mapper.readValue(mapper.writeValueAsString(event), AppointmentChangedEvent.class);

        assertThat(read).isEqualTo(event);
    }
}
