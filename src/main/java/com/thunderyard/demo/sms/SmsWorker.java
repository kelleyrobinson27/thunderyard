package com.thunderyard.demo.sms;

import com.thunderyard.demo.config.KafkaTopics;
import com.thunderyard.demo.notifications.Notification;
import com.thunderyard.demo.notifications.NotificationMetrics;
import com.thunderyard.demo.notifications.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SmsWorker {

    private static final Logger log = LoggerFactory.getLogger(SmsWorker.class);

    private final SmsProviderClient smsProviderClient;
    private final NotificationRepository notificationRepository;
    private final KafkaTemplate<String, SmsJob> kafkaTemplate;
    private final NotificationMetrics metrics;

    public SmsWorker(SmsProviderClient smsProviderClient,
                     NotificationRepository notificationRepository,
                     KafkaTemplate<String, SmsJob> kafkaTemplate,
                     NotificationMetrics metrics) {
        this.smsProviderClient = smsProviderClient;
        this.notificationRepository = notificationRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.metrics = metrics;
    }

    @KafkaListener(topics = KafkaTopics.SMS_JOBS, groupId = "notification-service")
    public void handleSmsJob(SmsJob job) {

        Optional<Notification> notificationOpt =
                notificationRepository.findById(job.getNotificationId());

        if (notificationOpt.isEmpty()) {
            log.warn("Notification not found for job correlationId={}", job.getCorrelationId());
            return;
        }

        Notification notification = notificationOpt.get();

        try {
            // 2. Attempt SMS send
            boolean success = smsProviderClient.sendSms(job);

            if (success) {
                notification.setStatus(Notification.Status.SENT);
                notificationRepository.save(notification);
                metrics.sent();
                return;
            }

            notification.setStatus(Notification.Status.FAILED);
            notification.setFailureReason("Provider returned failure");
            notificationRepository.save(notification);
            metrics.failed();

        } catch (Exception ex) {

            // 4. Retryable failure (network, timeout, transient)
            log.warn("Retryable SMS failure correlationId={}: {}", job.getCorrelationId(), ex.getMessage());

            int retryCount = job.getRetryCount() + 1;

            if (retryCount <= 3) {
                SmsJob retryJob = job.withRetryCount(retryCount);
                kafkaTemplate.send(KafkaTopics.SMS_JOBS, job.getCorrelationId(), retryJob);
                notification.setStatus(Notification.Status.RETRIED);
                notificationRepository.save(notification);
                metrics.retried();
                return;
            }

            log.warn("Sending job to DLQ correlationId={}", job.getCorrelationId());
            kafkaTemplate.send(KafkaTopics.SMS_JOBS_DLQ, job.getCorrelationId(), job);

            notification.setStatus(Notification.Status.FAILED);
            notification.setFailureReason("Retries exhausted");
            notificationRepository.save(notification);
            metrics.failed();
        }
    }
}
