package com.thunderyard.demo.sms;

import com.thunderyard.demo.notifications.Notification;
import com.thunderyard.demo.notifications.NotificationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SmsWorker {

    private final SmsProviderClient smsProviderClient;
    private final NotificationRepository notificationRepository;
    private final KafkaTemplate<String, SmsJob> kafkaTemplate;

    public SmsWorker(SmsProviderClient smsProviderClient,
                     NotificationRepository notificationRepository,
                     KafkaTemplate<String, SmsJob> kafkaTemplate) {
        this.smsProviderClient = smsProviderClient;
        this.notificationRepository = notificationRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "sms-jobs", groupId = "notification-service")
    public void handleSmsJob(SmsJob job) {

        // 1. Fetch notification record
        Optional<Notification> notificationOpt =
                notificationRepository.findById(job.getNotificationId());

        if (notificationOpt.isEmpty()) {
            System.out.println("Notification not found for job: " + job);
            return;
        }

        Notification notification = notificationOpt.get();

        try {
            // 2. Attempt SMS send
            boolean success = smsProviderClient.sendSms(job);

            if (success) {
                notification.setStatus(Notification.Status.SENT);
                notificationRepository.save(notification);
                return;
            }

            // 3. Permanent failure (provider returned false)
            notification.setStatus(Notification.Status.FAILED);
            notification.setFailureReason("Provider returned failure");
            notificationRepository.save(notification);

        } catch (Exception ex) {

            // 4. Retryable failure (network, timeout, transient)
            System.out.println("Retryable SMS failure: " + ex.getMessage());

            int retryCount = job.getRetryCount() + 1;

            if (retryCount <= 3) {
                // 5. Requeue with incremented retry count
                SmsJob retryJob = job.withRetryCount(retryCount);
                kafkaTemplate.send("sms-jobs", retryJob);
                notification.setStatus(Notification.Status.RETRIED);
                notificationRepository.save(notification);
                return;
            }

            // 6. Retries exhausted → DLQ
            System.out.println("Sending job to DLQ: " + job);
            kafkaTemplate.send("sms-jobs-dlq", job);

            notification.setStatus(Notification.Status.FAILED);
            notification.setFailureReason("Retries exhausted");
            notificationRepository.save(notification);
        }
    }
}
