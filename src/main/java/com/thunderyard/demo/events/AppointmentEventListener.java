package com.thunderyard.demo.events;

import com.thunderyard.demo.config.CorrelationIds;
import com.thunderyard.demo.config.KafkaTopics;
import com.thunderyard.demo.config.TraceContext;
import com.thunderyard.demo.notifications.Notification;
import com.thunderyard.demo.notifications.NotificationMetrics;
import com.thunderyard.demo.notifications.NotificationRepository;
import com.thunderyard.demo.events.persistence.EventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

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
        boolean owned = CorrelationIds.openIfAbsent(CorrelationIds.fromEventId(event.eventId()));
        try {
            eventStore.persist(event);
            boolean typeAllowsNotify = event.eventType() != AppointmentChangedEvent.EventType.CANCELLED;
            notifyIfEligible(event.eventId(), event.appointmentId(), event.patientId(), typeAllowsNotify);
        } finally {
            if (owned) {
                CorrelationIds.close();
            }
        }
    }

    public void handleEvent(AppointmentReminderEvent event) {
        eventStore.persist(event);
        notifyIfEligible(event.eventId(), event.appointmentId(), event.patientId(), true);
    }

    public void handleEvent(AppointmentCreatedEvent event) {
        eventStore.persist(event);
        notifyIfEligible(event.eventId(), event.appointmentId(), event.patientId(), true);
    }

    public void handleEvent(AppointmentCancelledEvent event) {
        eventStore.persist(event);
        notifyIfEligible(event.eventId(), event.appointmentId(), event.patientId(), false);
    }

    public void handleEvent(AppointmentStatusChangedEvent event) {
        eventStore.persist(event);
        notifyIfEligible(event.eventId(), event.appointmentId(), event.patientId(), true);
    }

    public void handleEvent(AppointmentConfirmationEvent event) {
        eventStore.persist(event);
        notifyIfEligible(event.eventId(), event.appointmentId(), event.patientId(), true);
    }

    private void notifyIfEligible(UUID eventId, UUID appointmentId, UUID patientId, boolean typeAllowsNotify) {
        boolean ownedTrace = TraceContext.openIfAbsent();
        try {
            notifyIfEligibleWithTrace(eventId, appointmentId, patientId, typeAllowsNotify);
        } finally {
            if (ownedTrace) {
                TraceContext.close();
            }
        }
    }

    private void notifyIfEligibleWithTrace(UUID eventId, UUID appointmentId, UUID patientId, boolean typeAllowsNotify) {
        Optional<Notification> existing = notificationRepository.findByEventId(eventId);
        if (existing.isPresent()) {
            metrics.duplicated();
            return;
        }

        AppointmentDetails appointment = easClient.getAppointment(appointmentId).join();

        if (!typeAllowsNotify || !appointment.canNotify()) {
            if (!appointment.canNotify()) {
                log.info("Suppressing eventId={} — EAS canNotify=false", eventId);
            }
            Notification suppressed = new Notification();
            suppressed.setEventId(eventId);
            suppressed.setAppointmentId(appointmentId);
            suppressed.setPatientId(patientId);
            suppressed.setStatus(Notification.Status.SUPPRESSED);
            suppressed.setTraceId(TraceContext.currentTraceId());
            notificationRepository.save(suppressed);
            metrics.suppressed();
            return;
        }

        Notification notification = new Notification();
        notification.setEventId(eventId);
        notification.setAppointmentId(appointmentId);
        notification.setPatientId(patientId);
        notification.setStatus(Notification.Status.QUEUED);
        notification.setTraceId(TraceContext.currentTraceId());
        notification = notificationRepository.save(notification);

        smsQueueProducer.publishSmsJob(notification);
        metrics.queued();
    }
}
