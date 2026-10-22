package com.thunderyard.demo.events;

import com.thunderyard.demo.events.persistence.EventStore;
import com.thunderyard.demo.events.persistence.EventRepository;
import com.thunderyard.demo.notifications.Notification;
import com.thunderyard.demo.notifications.NotificationMetrics;
import com.thunderyard.demo.notifications.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

class EventPersistenceIntegrationTest {

    private final NotificationRepository notificationRepo = mock(NotificationRepository.class);
    private final EasClient easClient = mock(EasClient.class);
    private final SmsQueueProducer smsProducer = mock(SmsQueueProducer.class);

    // EventStore MUST be mocked, not real
    private final EventStore eventStore = mock(EventStore.class);
    private final NotificationMetrics metrics = mock(NotificationMetrics.class);

    private final AppointmentEventListener listener =
            new AppointmentEventListener(notificationRepo, easClient, smsProducer, eventStore, metrics);

    @Test
    void listenerPersistsEventBeforeProcessing() {
        UUID eventId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();

        AppointmentChangedEvent evt = new AppointmentChangedEvent(
                eventId,
                appointmentId,
                patientId,
                AppointmentChangedEvent.EventType.UPDATED,
                Instant.now(),
                "integration-test"
        );

        when(notificationRepo.findByEventId(eventId)).thenReturn(Optional.empty());

        when(easClient.getAppointment(appointmentId)).thenReturn(
                CompletableFuture.completedFuture(
                        new AppointmentDetails(appointmentId, "Dr. Who", "2026-10-20", true)
                )
        );
        when(notificationRepo.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        listener.handleEvent(evt);

        // Verify persistence was called
        verify(eventStore).persist(evt);

        // Verify business logic still runs
        verify(notificationRepo).save(any());
        verify(smsProducer).publishSmsJob(any());
    }
}
