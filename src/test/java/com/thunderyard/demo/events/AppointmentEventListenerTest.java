package com.thunderyard.demo.events;

import com.thunderyard.demo.config.CorrelationIds;
import com.thunderyard.demo.notifications.Notification;
import com.thunderyard.demo.notifications.NotificationMetrics;
import com.thunderyard.demo.notifications.NotificationRepository;
import com.thunderyard.demo.events.persistence.EventStore;
import org.junit.jupiter.api.AfterEach;
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
    private final EventStore eventStore = mock(EventStore.class);
    private final NotificationMetrics metrics = mock(NotificationMetrics.class);

    private final AppointmentEventListener listener =
            new AppointmentEventListener(repo, easClient, smsProducer, eventStore, metrics);

    /**
     * Helper method to build a valid AppointmentChangedEvent
     * matching your actual record constructor.
     */
    @AfterEach
    void clearMdc() {
        CorrelationIds.close();
    }

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

    @Test
    void suppressesWhenEasSaysCannotNotify() {
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
                        new AppointmentDetails(appointmentId, "Dr. Smith", "2026-10-20", false)
                )
        );

        listener.handleEvent(evt);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repo).save(captor.capture());
        assert captor.getValue().getStatus() == Notification.Status.SUPPRESSED;
        verify(smsProducer, never()).publishSmsJob(any());
    }

    @Test
    void pinsCorrEventIdWhenMdcEmptyAndClearsAfter() {
        UUID eventId = UUID.randomUUID();
        AppointmentChangedEvent evt = event(
                eventId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                AppointmentChangedEvent.EventType.UPDATED
        );
        when(repo.findByEventId(eventId)).thenReturn(Optional.of(new Notification()));
        doAnswer(inv -> {
            assert CorrelationIds.current().equals(CorrelationIds.fromEventId(eventId));
            return null;
        }).when(eventStore).persist(evt);

        listener.handleEvent(evt);

        assert CorrelationIds.current() == null;
    }

    @Test
    void keepsInboundCorrelationId() {
        UUID eventId = UUID.randomUUID();
        AppointmentChangedEvent evt = event(
                eventId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                AppointmentChangedEvent.EventType.UPDATED
        );
        when(repo.findByEventId(eventId)).thenReturn(Optional.of(new Notification()));
        CorrelationIds.open("corr-inbound");
        doAnswer(inv -> {
            assert CorrelationIds.current().equals("corr-inbound");
            return null;
        }).when(eventStore).persist(evt);

        listener.handleEvent(evt);

        assert CorrelationIds.current().equals("corr-inbound");
    }

    @Test
    void persistsTypedEventsWithoutQueuingSms() {
        AppointmentReminderEvent reminder = new AppointmentReminderEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AppointmentReminderEvent.ReminderType.UPCOMING, Instant.now(), "test");
        AppointmentCreatedEvent created = new AppointmentCreatedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now(), "test");
        AppointmentCancelledEvent cancelled = new AppointmentCancelledEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AppointmentCancelledEvent.CancelReason.SYSTEM, Instant.now(), "test");
        AppointmentStatusChangedEvent status = new AppointmentStatusChangedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AppointmentStatusChangedEvent.Status.SCHEDULED,
                AppointmentStatusChangedEvent.Status.COMPLETED, Instant.now(), "test");
        AppointmentConfirmationEvent confirmation = new AppointmentConfirmationEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                AppointmentConfirmationEvent.ConfirmationType.PROVIDER_CONFIRMED, Instant.now(), "test");

        listener.handleEvent(reminder);
        listener.handleEvent(created);
        listener.handleEvent(cancelled);
        listener.handleEvent(status);
        listener.handleEvent(confirmation);

        verify(eventStore).persist(reminder);
        verify(eventStore).persist(created);
        verify(eventStore).persist(cancelled);
        verify(eventStore).persist(status);
        verify(eventStore).persist(confirmation);
        verify(smsProducer, never()).publishSmsJob(any());
    }
}
