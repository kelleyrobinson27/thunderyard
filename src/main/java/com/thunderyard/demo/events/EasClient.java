package com.thunderyard.demo.events;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class EasClient {

    private final WebClient webClient;

    public EasClient() {
        // WebClient is the modern, non-blocking HTTP client for Spring Boot
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:8081") // mock EAS service
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
