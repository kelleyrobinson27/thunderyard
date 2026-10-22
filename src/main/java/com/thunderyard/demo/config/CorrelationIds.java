package com.thunderyard.demo.config;

import org.slf4j.MDC;

import java.util.UUID;

public final class CorrelationIds {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private CorrelationIds() {
    }

    public static String currentOr(String fallback) {
        String existing = MDC.get(MDC_KEY);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        return fallback;
    }

    public static String current() {
        return MDC.get(MDC_KEY);
    }

    public static void open(String id) {
        if (id != null && !id.isBlank()) {
            MDC.put(MDC_KEY, id.trim());
        }
    }

    public static boolean openIfAbsent(String id) {
        if (current() != null && !current().isBlank()) {
            return false;
        }
        open(id);
        return true;
    }

    public static void close() {
        MDC.remove(MDC_KEY);
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    public static String fromEventId(UUID eventId) {
        return "corr-" + eventId;
    }

    public static void run(String id, Runnable work) {
        boolean owned = openIfAbsent(id);
        try {
            work.run();
        } finally {
            if (owned) {
                close();
            }
        }
    }
}
