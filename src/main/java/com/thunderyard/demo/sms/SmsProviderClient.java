package com.thunderyard.demo.sms;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock SMS provider. A real client would use WebClient plus {@code @TimeLimiter}
 * the same way {@code EasClient} does.
 *
 * Reserved patient IDs (local curl):
 * {@link #RETRY_PATIENT_ID} — throw (retryable / timeout)
 * {@link #PERM_PATIENT_ID} — return false (permanent, do not requeue)
 */
@Component
public class SmsProviderClient {

    public static final UUID RETRY_PATIENT_ID =
            UUID.fromString("00000000-0000-0000-0000-00000000aaa1");
    public static final UUID PERM_PATIENT_ID =
            UUID.fromString("00000000-0000-0000-0000-00000000aaa2");

    private static final Logger log = LoggerFactory.getLogger(SmsProviderClient.class);

    @Retry(name = "sms")
    @CircuitBreaker(name = "sms", fallbackMethod = "fallbackSend")
    public boolean sendSms(SmsJob job) {
        log.info("Sending SMS correlationId={}", job.getCorrelationId());

        if (RETRY_PATIENT_ID.equals(job.getPatientId())) {
            throw new RetryableSmsException("SMS provider timeout");
        }
        if (PERM_PATIENT_ID.equals(job.getPatientId())) {
            return false;
        }
        return true;
    }

    public boolean fallbackSend(SmsJob job, Throwable t) {
        log.warn("SMS provider fallback correlationId={}: {}", job.getCorrelationId(), t.toString());
        throw new RetryableSmsException("SMS provider unavailable", t);
    }
}
