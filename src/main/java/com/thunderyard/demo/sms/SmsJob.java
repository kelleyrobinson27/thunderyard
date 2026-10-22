package com.thunderyard.demo.sms;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
public class SmsJob {
    private UUID notificationId;
    private UUID patientId;
    private String phoneNumber;
    private String messageBody;
    private String correlationId;
    private int retryCount;

    public SmsJob withRetryCount(int newCount) {
        return this.toBuilder()
                .retryCount(newCount)
                .build();
    }
}
