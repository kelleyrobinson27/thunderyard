package com.thunderyard.demo.config;

import org.slf4j.MDC;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TraceContext {

    public static final String TRACEPARENT = "traceparent";
    public static final String TRACERESPONSE = "traceresponse";
    public static final String X_TRACE_ID = "X-Trace-Id";
    public static final String MDC_TRACE = "traceId";
    public static final String MDC_SPAN = "spanId";
    public static final String MDC_PARENT = "traceparent";

    private static final Pattern TRACEPARENT_PATTERN = Pattern.compile(
            "^00-([0-9a-f]{32})-([0-9a-f]{16})-([0-9a-f]{2})$"
    );

    private TraceContext() {
    }

    public record Parsed(String traceId, String spanId, String flags) {
        public String asTraceparent() {
            return "00-" + traceId + "-" + spanId + "-" + flags;
        }
    }

    public static Parsed parseOrCreate(String header) {
        if (header != null) {
            Matcher matcher = TRACEPARENT_PATTERN.matcher(header.trim());
            if (matcher.matches()) {
                String traceId = matcher.group(1);
                String spanId = matcher.group(2);
                String flags = matcher.group(3);
                if (!traceId.equals("0".repeat(32)) && !spanId.equals("0".repeat(16))) {
                    return new Parsed(traceId, spanId, flags);
                }
            }
        }
        return new Parsed(newTraceId(), newSpanId(), "01");
    }

    public static void open(Parsed parsed) {
        MDC.put(MDC_TRACE, parsed.traceId());
        MDC.put(MDC_SPAN, parsed.spanId());
        MDC.put(MDC_PARENT, parsed.asTraceparent());
    }

    public static boolean openIfAbsent() {
        if (currentTraceparent() != null && !currentTraceparent().isBlank()) {
            return false;
        }
        open(parseOrCreate(null));
        return true;
    }

    public static String currentTraceId() {
        return MDC.get(MDC_TRACE);
    }

    public static String currentTraceparent() {
        return MDC.get(MDC_PARENT);
    }

    public static void run(String traceparent, Runnable work) {
        boolean owned = currentTraceparent() == null || currentTraceparent().isBlank();
        if (owned) {
            open(parseOrCreate(traceparent));
        }
        try {
            work.run();
        } finally {
            if (owned) {
                close();
            }
        }
    }

    public static void close() {
        MDC.remove(MDC_TRACE);
        MDC.remove(MDC_SPAN);
        MDC.remove(MDC_PARENT);
    }

    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String newSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
