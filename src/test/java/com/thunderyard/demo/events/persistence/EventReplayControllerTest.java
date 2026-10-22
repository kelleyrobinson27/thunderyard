package com.thunderyard.demo.events.persistence;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventReplayControllerTest {

    private final EventReplayService replayService = mock(EventReplayService.class);
    private final EventReplayController controller = new EventReplayController(replayService);

    @Test
    void replayAllReturnsCount() {
        when(replayService.replayAll()).thenReturn(3);
        assertThat(controller.replayAll()).isEqualTo("Replayed 3 events.");
    }

    @Test
    void replayByTypeReturnsCount() {
        when(replayService.replayByType("AppointmentChangedEvent")).thenReturn(2);
        assertThat(controller.replayByType("AppointmentChangedEvent"))
                .isEqualTo("Replayed 2 events of type AppointmentChangedEvent.");
    }
}
