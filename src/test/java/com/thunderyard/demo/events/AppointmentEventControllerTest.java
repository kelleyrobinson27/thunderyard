package com.thunderyard.demo.events;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AppointmentEventControllerTest {

    private final AppointmentEventListener listener = mock(AppointmentEventListener.class);
    private final AppointmentEventController controller = new AppointmentEventController(listener);

    @Test
    void delegatesChangedEvent() {
        AppointmentChangedEvent event = new AppointmentChangedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AppointmentChangedEvent.EventType.UPDATED, Instant.now(), "test");
        controller.handleAppointmentChanged(event);
        verify(listener).handleEvent(event);
    }

    @Test
    void delegatesReminderEvent() {
        AppointmentReminderEvent event = new AppointmentReminderEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AppointmentReminderEvent.ReminderType.UPCOMING, Instant.now(), "test");
        controller.handleAppointmentReminder(event);
        verify(listener).handleEvent(event);
    }

    @Test
    void delegatesCreatedEvent() {
        AppointmentCreatedEvent event = new AppointmentCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now(), "test");
        controller.handleAppointmentCreated(event);
        verify(listener).handleEvent(event);
    }

    @Test
    void delegatesCancelledEvent() {
        AppointmentCancelledEvent event = new AppointmentCancelledEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AppointmentCancelledEvent.CancelReason.PATIENT_REQUEST, Instant.now(), "test");
        controller.handleAppointmentCancelled(event);
        verify(listener).handleEvent(event);
    }

    @Test
    void delegatesStatusChangedEvent() {
        AppointmentStatusChangedEvent event = new AppointmentStatusChangedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AppointmentStatusChangedEvent.Status.SCHEDULED,
                AppointmentStatusChangedEvent.Status.IN_PROGRESS, Instant.now(), "test");
        controller.handleAppointmentStatusChanged(event);
        verify(listener).handleEvent(event);
    }

    @Test
    void delegatesConfirmationEvent() {
        AppointmentConfirmationEvent event = new AppointmentConfirmationEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AppointmentConfirmationEvent.ConfirmationType.PATIENT_CONFIRMED, Instant.now(), "test");
        controller.handleAppointmentConfirmation(event);
        verify(listener).handleEvent(event);
    }
}
