package com.thunderyard.demo.sms;

import com.thunderyard.demo.config.CorrelationIds;
import com.thunderyard.demo.notifications.Notification;
import com.thunderyard.demo.notifications.NotificationMetrics;
import com.thunderyard.demo.notifications.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/sms")
public class DeliveryCallbackController {

    private static final Logger log = LoggerFactory.getLogger(DeliveryCallbackController.class);

    private final NotificationRepository notificationRepository;
    private final NotificationMetrics metrics;

    public DeliveryCallbackController(NotificationRepository notificationRepository,
                                      NotificationMetrics metrics) {
        this.notificationRepository = notificationRepository;
        this.metrics = metrics;
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> handleCallback(@RequestBody DeliveryStatusCallback callback) {
        CorrelationIds.openIfAbsent(callback.getCorrelationId());

        log.info("Delivery callback received corr={} status={}",
                callback.getCorrelationId(), callback.getStatus());

        Optional<Notification> notificationOpt =
                notificationRepository.findById(callback.getNotificationId());

        if (notificationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Notification notification = notificationOpt.get();

        switch (callback.getStatus()) {
            case "DELIVERED" -> {
                notification.setStatus(Notification.Status.DELIVERED);
                metrics.delivered();
            }
            case "FAILED" -> {
                notification.setStatus(Notification.Status.FAILED);
                notification.setFailureReason(callback.getProviderCode());
                metrics.failed();
            }
            case "UNDELIVERABLE" -> {
                notification.setStatus(Notification.Status.FAILED);
                notification.setFailureReason("UNDELIVERABLE: " + callback.getProviderCode());
                metrics.failed();
            }
            default -> {
                notification.setStatus(Notification.Status.FAILED);
                notification.setFailureReason("Unknown status: " + callback.getStatus());
                metrics.failed();
            }
        }

        notificationRepository.save(notification);

        return ResponseEntity.ok().build();
    }
}
