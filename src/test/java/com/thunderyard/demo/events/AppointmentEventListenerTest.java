package com.thunderyard.demo.events;

import com.thunderyard.demo.notifications.Notification;
import com.thunderyard.demo.notifications.NotificationRepository;
import com.thunderyard.demo.events.persistence.EventStore;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

/**
 * Unit tests for AppointmentEventListener.
 *
 * These tests validate:
 *  - Idempotency (duplicate events are ignored)
 *  - Suppression logic (CANCELLED events do not trigger notifications)
 *  - Notification creation for valid events
 *  - SMS job publishing
 */
class AppointmentEventListenerTest {

    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final EasClient easClient = mock(EasClient.class);
    private final SmsQueueProducer smsProducer = mock(SmsQueueProducer.class);
    private final EventStore eventStore = mock(EventStore.class);   // <-- NEW

    private final AppointmentEventListener listener =
            new AppointmentEventListener(repo, easClient, smsProducer, eventStore);  // <-- UPDATED

    /**
     * Helper method to build a valid AppointmentChangedEvent
     * matching your actual record constructor.
     */
    private AppointmentChangedEvent event(
            UUID eventId,
            UUID appointmentId,
            UUID patientId,
            AppointmentChangedEvent.EventType type
    ) {
        return new AppointmentChangedEvent(
                eventId,
                appointmentId,
                patientId,
                type,
                Instant.now(),       // required field
                "test-suite"         // required field
        );
    }

    @Test
    void ignoresDuplicateEvents() {
        UUID eventId = UUID.randomUUID();

        AppointmentChangedEvent evt = event(
                eventId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                AppointmentChangedEvent.EventType.UPDATED
        );

        when(repo.findByEventId(eventId)).thenReturn(Optional.of(new Notification()));

        listener.handleEvent(evt);

        verify(repo, never()).save(any());
        verify(smsProducer, never()).publishSmsJob(any());
        verify(eventStore).persist(evt);   // <-- NEW: ensure persistence still happens
    }

    @Test
    void suppressesCancellationEvents() {
        UUID eventId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        AppointmentChangedEvent evt = event(
                eventId,
                appointmentId,
                patientId,
                AppointmentChangedEvent.EventType.CANCELLED
        );

        when(repo.findByEventId(eventId)).thenReturn(Optional.empty());

        when(easClient.getAppointment(appointmentId)).thenReturn(
                CompletableFuture.completedFuture(
                        new AppointmentDetails(appointmentId, null, null, false)
                )
        );

        listener.handleEvent(evt);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repo).save(captor.capture());

        Notification saved = captor.getValue();
        assert saved.getStatus() == Notification.Status.SUPPRESSED;

        verify(smsProducer, never()).publishSmsJob(any());
        verify(eventStore).persist(evt);   // <-- NEW
    }

    @Test
    void createsNotificationAndPublishesSmsJob() {
        UUID eventId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        AppointmentChangedEvent evt = event(
                eventId,
                appointmentId,
                patientId,
                AppointmentChangedEvent.EventType.UPDATED
        );

        when(repo.findByEventId(eventId)).thenReturn(Optional.empty());

        when(easClient.getAppointment(appointmentId)).thenReturn(
                CompletableFuture.completedFuture(
                        new AppointmentDetails(appointmentId, "Dr. Smith", "2026-10-20", true)
                )
        );
        when(repo.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        listener.handleEvent(evt);

        verify(repo).save(any(Notification.class));
        verify(smsProducer).publishSmsJob(any(Notification.class));
        verify(eventStore).persist(evt);   // <-- NEW
    }
}
