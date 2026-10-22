package com.thunderyard.demo.events;

import com.thunderyard.demo.events.persistence.ReplayTooLargeException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsIllegalArgumentTo400() {
        ResponseEntity<String> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Unknown event type: nope"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Unknown event type: nope");
    }

    @Test
    void mapsReplayTooLargeTo413() {
        ResponseEntity<String> response = handler.handleReplayTooLarge(new ReplayTooLargeException(119));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isEqualTo("Replay limited to 50 events (got 119)");
    }
}
