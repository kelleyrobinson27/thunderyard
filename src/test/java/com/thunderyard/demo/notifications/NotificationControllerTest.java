package com.thunderyard.demo.notifications;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationControllerTest {

    private final NotificationRepository repo = mock(NotificationRepository.class);
    private final NotificationController controller = new NotificationController(repo);

    @Test
    void returnsNotificationForEventId() {
        UUID eventId = UUID.randomUUID();
        Notification n = new Notification();
        n.setEventId(eventId);
        n.setNotificationId(UUID.randomUUID());
        when(repo.findByEventId(eventId)).thenReturn(Optional.of(n));

        ResponseEntity<Notification> response = controller.getByEventId(eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(n);
    }

    @Test
    void returns404WhenMissing() {
        UUID eventId = UUID.randomUUID();
        when(repo.findByEventId(eventId)).thenReturn(Optional.empty());

        ResponseEntity<Notification> response = controller.getByEventId(eventId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
