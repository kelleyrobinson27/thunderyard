package com.thunderyard.demo.events;

import com.thunderyard.demo.config.CorrelationIds;
import com.thunderyard.demo.config.KafkaTopics;
import com.thunderyard.demo.notifications.Notification;
import com.thunderyard.demo.sms.SmsJob;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmsQueueProducerTest {

    @Test
    void publishesSmsJobToKafkaPartitionedByAppointment() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, SmsJob> kafka = mock(KafkaTemplate.class);
        when(kafka.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        SmsQueueProducer producer = new SmsQueueProducer(kafka);

        UUID appointmentId = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID());
        notification.setEventId(UUID.randomUUID());
        notification.setAppointmentId(appointmentId);
        notification.setPatientId(UUID.randomUUID());

        producer.publishSmsJob(notification);

        ArgumentCaptor<SmsJob> jobCaptor = ArgumentCaptor.forClass(SmsJob.class);
        verify(kafka).send(eq(KafkaTopics.SMS_JOBS), eq(appointmentId.toString()), jobCaptor.capture());

        SmsJob job = jobCaptor.getValue();
        assertThat(job.getNotificationId()).isEqualTo(notification.getNotificationId());
        assertThat(job.getCorrelationId()).isEqualTo("corr-" + notification.getEventId());
        assertThat(job.getRetryCount()).isZero();
    }

    @Test
    void usesMdcCorrelationIdWhenPresent() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, SmsJob> kafka = mock(KafkaTemplate.class);
        when(kafka.send(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        SmsQueueProducer producer = new SmsQueueProducer(kafka);

        Notification notification = new Notification();
        notification.setNotificationId(UUID.randomUUID());
        notification.setEventId(UUID.randomUUID());
        notification.setAppointmentId(UUID.randomUUID());
        notification.setPatientId(UUID.randomUUID());

        CorrelationIds.open("corr-from-http");
        try {
            producer.publishSmsJob(notification);
        } finally {
            CorrelationIds.close();
        }

        ArgumentCaptor<SmsJob> jobCaptor = ArgumentCaptor.forClass(SmsJob.class);
        verify(kafka).send(eq(KafkaTopics.SMS_JOBS), any(), jobCaptor.capture());
        assertThat(jobCaptor.getValue().getCorrelationId()).isEqualTo("corr-from-http");
    }
}
