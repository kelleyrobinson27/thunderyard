package com.thunderyard.demo.sms;

import com.thunderyard.demo.config.CorrelationIds;
import com.thunderyard.demo.notifications.Notification;
import com.thunderyard.demo.notifications.NotificationMetrics;
import com.thunderyard.demo.notifications.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class DeliveryCallbackControllerTest {

    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final NotificationMetrics metrics = mock(NotificationMetrics.class);
    private final DeliveryCallbackController controller =
            new DeliveryCallbackController(repo, metrics);

    @AfterEach
    void clearMdc() {
        CorrelationIds.close();
    }

    @Test
    void marksDelivered() {
        UUID id = UUID.randomUUID();
        Notification n = new Notification();
        n.setNotificationId(id);

        DeliveryStatusCallback cb = new DeliveryStatusCallback();
        cb.setNotificationId(id);
        cb.setStatus("DELIVERED");

        when(repo.findById(id)).thenReturn(Optional.of(n));

        controller.handleCallback(cb);

        assert n.getStatus() == Notification.Status.DELIVERED;
        verify(repo).save(n);
    }

    @Test
    void marksFailed() {
        UUID id = UUID.randomUUID();
        Notification n = new Notification();
        n.setNotificationId(id);

        DeliveryStatusCallback cb = new DeliveryStatusCallback();
        cb.setNotificationId(id);
        cb.setStatus("FAILED");
        cb.setProviderCode("404");

        when(repo.findById(id)).thenReturn(Optional.of(n));

        controller.handleCallback(cb);

        assert n.getStatus() == Notification.Status.FAILED;
        assert n.getFailureReason().equals("404");
    }

    @Test
    void opensMdcFromCallbackCorrelationId() {
        UUID id = UUID.randomUUID();
        Notification n = new Notification();
        n.setNotificationId(id);
        DeliveryStatusCallback cb = new DeliveryStatusCallback();
        cb.setNotificationId(id);
        cb.setStatus("DELIVERED");
        cb.setCorrelationId("corr-callback");
        when(repo.findById(id)).thenReturn(Optional.of(n));
        when(repo.save(n)).thenAnswer(inv -> {
            assert CorrelationIds.current().equals("corr-callback");
            return n;
        });

        controller.handleCallback(cb);

        assert n.getStatus() == Notification.Status.DELIVERED;
    }

    @Test
    void returns404WhenNotificationMissing() {
        UUID id = UUID.randomUUID();
        DeliveryStatusCallback cb = new DeliveryStatusCallback();
        cb.setNotificationId(id);
        cb.setStatus("DELIVERED");
        when(repo.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = controller.handleCallback(cb);

        assert response.getStatusCode() == HttpStatus.NOT_FOUND;
        verify(repo, never()).save(any());
    }

    @Test
    void marksUndeliverableAsFailed() {
        UUID id = UUID.randomUUID();
        Notification n = new Notification();
        n.setNotificationId(id);
        DeliveryStatusCallback cb = new DeliveryStatusCallback();
        cb.setNotificationId(id);
        cb.setStatus("UNDELIVERABLE");
        cb.setProviderCode("410");
        when(repo.findById(id)).thenReturn(Optional.of(n));

        controller.handleCallback(cb);

        assert n.getStatus() == Notification.Status.FAILED;
        assert n.getFailureReason().equals("UNDELIVERABLE: 410");
        verify(metrics).failed();
    }
}
