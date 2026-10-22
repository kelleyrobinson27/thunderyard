package com.thunderyard.demo.sms;

import lombok.Data;

import java.util.UUID;

@Data
public class DeliveryStatusCallback {
    private UUID notificationId;
    private String status;        // "DELIVERED", "FAILED", "UNDELIVERABLE"
    private String providerCode;  // optional carrier error code
    private String correlationId; // for tracing
}
