package com.thunderyard.demo.events;

import com.thunderyard.demo.notifications.Notification;
import com.thunderyard.demo.sms.SmsJob;
import org.springframework.stereotype.Component;

@Component
public class SmsQueueProducer {

    public void publishSmsJob(Notification notification) {
        SmsJob job = SmsJob.builder()
                .notificationId(notification.getNotificationId())
                .patientId(notification.getPatientId())
                .phoneNumber("555-123-4567") // mock for now
                .messageBody("Your appointment has been updated.")
                .correlationId("corr-" + notification.getEventId())
                .retryCount(0)
                .build();

        System.out.println("Queued SMS job: " + job);
        // Later: send to Kafka
    }
}
