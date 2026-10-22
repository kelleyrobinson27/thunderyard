package com.thunderyard.demo.sms;

import com.thunderyard.demo.config.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DlqWorker {

    private static final Logger log = LoggerFactory.getLogger(DlqWorker.class);

    @KafkaListener(topics = KafkaTopics.SMS_JOBS_DLQ, groupId = "notification-service")
    public void handleDlq(SmsJob job) {
        log.warn("DLQ received job correlationId={} notificationId={}",
                job.getCorrelationId(), job.getNotificationId());
    }
}
