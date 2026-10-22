package com.thunderyard.demo.sms;

import com.thunderyard.demo.config.CorrelationIds;
import com.thunderyard.demo.config.TraceContext;
import com.thunderyard.demo.notifications.Notification;
import com.thunderyard.demo.notifications.NotificationMetrics;
import com.thunderyard.demo.notifications.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class SmsWorkerTest {

    private final SmsProviderClient provider = mock(SmsProviderClient.class);
    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final KafkaTemplate<String, SmsJob> kafka = mock(KafkaTemplate.class);
    private final NotificationMetrics metrics = mock(NotificationMetrics.class);

    private final SmsWorker worker = new SmsWorker(provider, repo, kafka, metrics);

    @AfterEach
    void clearMdc() {
        CorrelationIds.close();
        TraceContext.close();
    }

    @Test
    void marksNotificationSentOnSuccess() {
        UUID id = UUID.randomUUID();
        Notification n = new Notification();
        n.setNotificationId(id);

        SmsJob job = SmsJob.builder()
                .notificationId(id)
                .retryCount(0)
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(n));
        when(provider.sendSms(job)).thenReturn(true);

        worker.handleSmsJob(job);

        assert n.getStatus() == Notification.Status.SENT;
        verify(repo).save(n);
    }

    @Test
    void retriesOnException() {
        UUID id = UUID.randomUUID();
        Notification n = new Notification();
        n.setNotificationId(id);

        SmsJob job = SmsJob.builder()
                .notificationId(id)
                .retryCount(0)
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(n));
        when(provider.sendSms(job)).thenThrow(new RuntimeException("network"));

        worker.handleSmsJob(job);

        verify(kafka).send(eq("sms-jobs"), any(), any(SmsJob.class));
        assert n.getStatus() == Notification.Status.RETRIED;
    }

    @Test
    void sendsToDlqAfterRetriesExhausted() {
        UUID id = UUID.randomUUID();
        Notification n = new Notification();
        n.setNotificationId(id);

        SmsJob job = SmsJob.builder()
                .notificationId(id)
                .retryCount(3)
                .build();

        when(repo.findById(id)).thenReturn(Optional.of(n));
        when(provider.sendSms(job)).thenThrow(new RuntimeException("network"));

        worker.handleSmsJob(job);

        verify(kafka).send(eq("sms-jobs-dlq"), any(), eq(job));
        assert n.getStatus() == Notification.Status.FAILED;
    }

    @Test
    void putsJobCorrelationIdInMdcWhileSending() {
        UUID id = UUID.randomUUID();
        Notification n = new Notification();
        n.setNotificationId(id);
        SmsJob job = SmsJob.builder()
                .notificationId(id)
                .correlationId("corr-sms")
                .retryCount(0)
                .build();
        when(repo.findById(id)).thenReturn(Optional.of(n));
        when(provider.sendSms(job)).thenAnswer(inv -> {
            assert CorrelationIds.current().equals("corr-sms");
            return true;
        });

        worker.handleSmsJob(job);

        assert CorrelationIds.current() == null;
    }

    @Test
    void putsJobTraceparentInMdcWhileSending() {
        UUID id = UUID.randomUUID();
        Notification n = new Notification();
        n.setNotificationId(id);
        String header = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
        SmsJob job = SmsJob.builder()
                .notificationId(id)
                .traceparent(header)
                .retryCount(0)
                .build();
        when(repo.findById(id)).thenReturn(Optional.of(n));
        when(provider.sendSms(job)).thenAnswer(inv -> {
            assert TraceContext.currentTraceId().equals("4bf92f3577b34da6a3ce929d0e0e4736");
            return true;
        });

        worker.handleSmsJob(job);

        assert TraceContext.currentTraceId() == null;
    }

    @Test
    void skipsWhenNotificationMissing() {
        UUID id = UUID.randomUUID();
        SmsJob job = SmsJob.builder()
                .notificationId(id)
                .correlationId("corr-missing")
                .build();
        when(repo.findById(id)).thenReturn(Optional.empty());

        worker.handleSmsJob(job);

        verify(provider, never()).sendSms(any());
        verify(repo, never()).save(any());
    }
}
