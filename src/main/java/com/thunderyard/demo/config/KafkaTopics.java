package com.thunderyard.demo.config;

public final class KafkaTopics {

    public static final String APPOINTMENT_EVENTS = "appointment-events";
    public static final String SMS_JOBS = "sms-jobs";
    public static final String SMS_JOBS_DLQ = "sms-jobs-dlq";

    private KafkaTopics() {}
}
