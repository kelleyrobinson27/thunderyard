package com.thunderyard.demo.events;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class EasClient {

    /** Stub opt-out appointment id for local curl (canNotify=false). */
    public static final UUID OPT_OUT_APPOINTMENT_ID =
            UUID.fromString("00000000-0000-0000-0000-00000000ffff");

    private final WebClient webClient;
    private final boolean stub;

    public EasClient(
            @Value("${eas.base-url:http://localhost:8081}") String baseUrl,
            @Value("${eas.stub:false}") boolean stub
    ) {
        this.stub = stub;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Fetch appointment details from the Enterprise Appointment Service (EAS).
     *
     * This method demonstrates:
     *  - Timeouts (TimeLimiter)
     *  - Retries for transient failures (Retry)
     *  - Circuit breaker for downstream outages (CircuitBreaker)
     *  - Graceful fallback for permanent failures
     *
     * Returning CompletableFuture allows Resilience4j's TimeLimiter to wrap async calls.
     */
    @TimeLimiter(name = "eas")
    @Retry(name = "eas")
    @CircuitBreaker(name = "eas", fallbackMethod = "fallbackAppointment")
    public CompletableFuture<AppointmentDetails> getAppointment(UUID appointmentId) {

        if (stub) {
            boolean canNotify = !OPT_OUT_APPOINTMENT_ID.equals(appointmentId);
            log.info("EAS stub canNotify={} for appointment {}", canNotify, appointmentId);
            return CompletableFuture.completedFuture(
                    new AppointmentDetails(appointmentId, "Local clinic", "2026-10-22", canNotify)
            );
        }

        log.info("Calling EAS for appointment {}", appointmentId);

        return webClient.get()
                .uri("/appointments/{id}", appointmentId)
                .retrieve()
                .bodyToMono(AppointmentDetails.class)
                // Hard timeout at the WebClient level
                .timeout(Duration.ofSeconds(2))
                .doOnError(err -> log.error("EAS call failed: {}", err.getMessage()))
                .toFuture();
    }

    /**
     * Fallback method triggered when:
     *  - Circuit breaker is OPEN
     *  - Retries are exhausted
     *  - Timeouts occur
     *  - Permanent failures (4xx)
     *
     * This prevents your service from crashing and allows you to:
     *  - Mark notifications as FAILED
     *  - Suppress notifications safely
     *  - Avoid duplicate sends
     */
    public CompletableFuture<AppointmentDetails> fallbackAppointment(
            UUID appointmentId,
            Throwable throwable
    ) {
        log.warn("Fallback triggered for appointment {} due to {}", appointmentId, throwable.toString());

        // Return a safe fallback object
        AppointmentDetails fallback = new AppointmentDetails(
                appointmentId,
                null,
                null,
                false // cannot notify without real data
        );

        return CompletableFuture.completedFuture(fallback);
    }
}
