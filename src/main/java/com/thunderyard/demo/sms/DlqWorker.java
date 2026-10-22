package com.thunderyard.demo.sms;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DlqWorker {

    @KafkaListener(topics = "sms-jobs-dlq", groupId = "notification-service")
    public void handleDlq(SmsJob job) {
        System.out.println("DLQ received job: " + job);

        // In a real system:
        // - You might alert DevOps
        // - Store in S3
        // - Trigger manual replay
        // - Or send to a human review queue
    }
}
