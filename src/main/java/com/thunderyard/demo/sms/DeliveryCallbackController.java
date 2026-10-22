package com.thunderyard.demo.sms;

import com.thunderyard.demo.notifications.Notification;
import com.thunderyard.demo.notifications.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/sms")
public class DeliveryCallbackController {

    private final NotificationRepository notificationRepository;

    public DeliveryCallbackController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestBody DeliveryStatusCallback callback) {

        // Step 6.4 — correlation ID logging
        System.out.println("Delivery callback received: corr=" + callback.getCorrelationId());

        Optional<Notification> notificationOpt =
                notificationRepository.findById(callback.getNotificationId());

        if (notificationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Notification notification = notificationOpt.get();

        switch (callback.getStatus()) {
            case "DELIVERED" -> notification.setStatus(Notification.Status.DELIVERED);
            case "FAILED" -> {
                notification.setStatus(Notification.Status.FAILED);
                notification.setFailureReason(callback.getProviderCode());
            }
            case "UNDELIVERABLE" -> {
                notification.setStatus(Notification.Status.FAILED);
                notification.setFailureReason("UNDELIVERABLE: " + callback.getProviderCode());
            }
            default -> {
                notification.setStatus(Notification.Status.FAILED);
                notification.setFailureReason("Unknown status: " + callback.getStatus());
            }
        }

        notificationRepository.save(notification);

        return ResponseEntity.ok().build();
    }
}
