package com.thunderyard.demo.sms;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmsProviderClientTest {

    private final SmsProviderClient client = new SmsProviderClient();

    @Test
    void successReturnsTrue() {
        SmsJob job = SmsJob.builder()
                .patientId(UUID.randomUUID())
                .correlationId("corr-ok")
                .build();

        assertThat(client.sendSms(job)).isTrue();
    }

    @Test
    void retryPatientIdThrowsRetryable() {
        SmsJob job = SmsJob.builder()
                .patientId(SmsProviderClient.RETRY_PATIENT_ID)
                .correlationId("corr-retry")
                .build();

        assertThatThrownBy(() -> client.sendSms(job))
                .isInstanceOf(RetryableSmsException.class)
                .hasMessageContaining("timeout");
    }

    @Test
    void permanentPatientIdReturnsFalse() {
        SmsJob job = SmsJob.builder()
                .patientId(SmsProviderClient.PERM_PATIENT_ID)
                .correlationId("corr-perm")
                .build();

        assertThat(client.sendSms(job)).isFalse();
    }
}
