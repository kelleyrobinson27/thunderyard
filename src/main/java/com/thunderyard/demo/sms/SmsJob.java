package com.thunderyard.demo.sms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
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
