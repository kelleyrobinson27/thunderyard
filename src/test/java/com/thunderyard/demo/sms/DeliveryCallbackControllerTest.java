package com.thunderyard.demo.sms;

import com.thunderyard.demo.notifications.Notification;
import com.thunderyard.demo.notifications.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

class DeliveryCallbackControllerTest {

    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final DeliveryCallbackController controller =
            new DeliveryCallbackController(repo);

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
}
