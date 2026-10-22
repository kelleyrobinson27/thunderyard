package com.thunderyard.demo.events;

import com.thunderyard.demo.config.KafkaTopics;
import com.thunderyard.demo.notifications.Notification;
import com.thunderyard.demo.notifications.NotificationMetrics;
import com.thunderyard.demo.notifications.NotificationRepository;
import com.thunderyard.demo.events.persistence.EventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AppointmentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventListener.class);

    private final NotificationRepository notificationRepository;
    private final EasClient easClient;
    private final SmsQueueProducer smsQueueProducer;
    private final EventStore eventStore;
    private final NotificationMetrics metrics;

    public AppointmentEventListener(NotificationRepository notificationRepository,
                                    EasClient easClient,
                                    SmsQueueProducer smsQueueProducer,
                                    EventStore eventStore,
                                    NotificationMetrics metrics) {
        this.notificationRepository = notificationRepository;
        this.easClient = easClient;
        this.smsQueueProducer = smsQueueProducer;
        this.eventStore = eventStore;
        this.metrics = metrics;
    }

    @KafkaListener(topics = KafkaTopics.APPOINTMENT_EVENTS, groupId = "notification-service")
    public void handleEvent(AppointmentChangedEvent event) {

        // 0. Persist event
        eventStore.persist(event);   // <-- NEW

        // 1. Idempotency check
        Optional<Notification> existing = notificationRepository.findByEventId(event.eventId());
        if (existing.isPresent()) {
            metrics.duplicated();
            return;
        }

        // 2. Fetch appointment details from EAS (WebClient returns CompletableFuture)
        AppointmentDetails appointment = easClient
                .getAppointment(event.appointmentId())
                .join(); // <-- FIX: wait for async result

        // 3. Decide whether notification is needed
        if (!shouldNotify(event, appointment)) {
            Notification suppressed = new Notification();
            suppressed.setEventId(event.eventId());
            suppressed.setAppointmentId(event.appointmentId());
            suppressed.setPatientId(event.patientId());
            suppressed.setStatus(Notification.Status.SUPPRESSED);
            notificationRepository.save(suppressed);
            metrics.suppressed();
            return;
        }

        // 4. Create notification record
        Notification notification = new Notification();
        notification.setEventId(event.eventId());
        notification.setAppointmentId(event.appointmentId());
        notification.setPatientId(event.patientId());
        notification.setStatus(Notification.Status.QUEUED);
        notification = notificationRepository.save(notification);

        smsQueueProducer.publishSmsJob(notification);
        metrics.queued();
    }

    private boolean shouldNotify(AppointmentChangedEvent event, AppointmentDetails appointment) {
        if (!appointment.canNotify()) {
            log.info("Suppressing eventId={} — EAS canNotify=false", event.eventId());
            return false;
        }
        return switch (event.eventType()) {
            case CREATED, UPDATED -> true;
            case CANCELLED -> false;
        };
    }

    public void handleEvent(AppointmentReminderEvent event) {
        eventStore.persist(event);   // <-- NEW
        log.warn(">>> Listener processing AppointmentReminderEvent: {}", event);
        // TODO: implement reminder-specific logic
    }

    public void handleEvent(AppointmentCreatedEvent event) {
        eventStore.persist(event);   // <-- NEW
        log.warn(">>> Listener processing AppointmentCreatedEvent: {}", event);
    }

    public void handleEvent(AppointmentCancelledEvent event) {
        eventStore.persist(event);   // <-- NEW
        log.warn(">>> Listener processing AppointmentCancelledEvent: {}", event);
    }

    public void handleEvent(AppointmentStatusChangedEvent event) {
        eventStore.persist(event);   // <-- NEW
        log.warn(">>> Listener processing AppointmentStatusChangedEvent: {}", event);
    }

    public void handleEvent(AppointmentConfirmationEvent event) {
        eventStore.persist(event);   // <-- NEW
        log.warn(">>> Listener processing AppointmentConfirmationEvent: {}", event);
    }
}
