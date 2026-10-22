package com.thunderyard.demo.events.persistence;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events/replay")
public class EventReplayController {

    private final EventReplayService replayService;

    public EventReplayController(EventReplayService replayService) {
        this.replayService = replayService;
    }

    @PostMapping("/all")
    public String replayAll() {
        int count = replayService.replayAll();
        return "Replayed " + count + " events.";
    }

    @PostMapping("/{type}")
    public String replayByType(@PathVariable String type) {
        int count = replayService.replayByType(type);
        return "Replayed " + count + " events of type " + type + ".";
    }
}
