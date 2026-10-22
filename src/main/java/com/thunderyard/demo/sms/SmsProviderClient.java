package com.thunderyard.demo.sms;

import org.springframework.stereotype.Component;

@Component
public class SmsProviderClient {

    public boolean sendSms(SmsJob job) {
        System.out.println("Sending SMS to " + job.getPhoneNumber());
        // Mock success
        return true;
    }
}
