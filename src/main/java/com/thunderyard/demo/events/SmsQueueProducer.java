package com.thunderyard.demo.events;

import com.thunderyard.demo.config.KafkaTopics;
import com.thunderyard.demo.notifications.Notification;
import com.thunderyard.demo.sms.SmsJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class SmsQueueProducer {

    private static final Logger log = LoggerFactory.getLogger(SmsQueueProducer.class);

    private final KafkaTemplate<String, SmsJob> kafkaTemplate;

    public SmsQueueProducer(KafkaTemplate<String, SmsJob> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishSmsJob(Notification notification) {
        SmsJob job = SmsJob.builder()
                .notificationId(notification.getNotificationId())
                .patientId(notification.getPatientId())
                .phoneNumber("555-123-4567")
                .messageBody("Your appointment has been updated.")
                .correlationId("corr-" + notification.getEventId())
                .retryCount(0)
                .build();

        String key = notification.getAppointmentId().toString();
        try {
            kafkaTemplate.send(KafkaTopics.SMS_JOBS, key, job)
                    .get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to queue SMS job correlationId={}", job.getCorrelationId(), e);
            throw new IllegalStateException("Failed to publish SMS job to Kafka", e);
        }
        log.info("Queued SMS job notificationId={} correlationId={}",
                job.getNotificationId(), job.getCorrelationId());
    }
}
