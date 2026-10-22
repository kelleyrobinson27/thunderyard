package com.thunderyard.demo.events;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
public class AppointmentEventController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventController.class);

    private final AppointmentEventListener listener;

    public AppointmentEventController(AppointmentEventListener listener) {
        this.listener = listener;
        log.warn(">>> AppointmentEventController initialized");
    }

    @PostMapping("/appointment-changed")
    public void handleAppointmentChanged(@Valid @RequestBody AppointmentChangedEvent event) {
        log.warn(">>> HTTP endpoint received AppointmentChangedEvent: {}", event);
        listener.handleEvent(event);
    }

    @PostMapping("/appointment-reminder")
    public void handleAppointmentReminder(@Valid @RequestBody AppointmentReminderEvent event) {
        log.warn(">>> HTTP endpoint received AppointmentReminderEvent: {}", event);
        listener.handleEvent(event);
    }

    @PostMapping("/appointment-created")
    public void handleAppointmentCreated(@Valid @RequestBody AppointmentCreatedEvent event) {
        log.warn(">>> HTTP endpoint received AppointmentCreatedEvent: {}", event);
        listener.handleEvent(event);
    }

    @PostMapping("/appointment-cancelled")
    public void handleAppointmentCancelled(@Valid @RequestBody AppointmentCancelledEvent event) {
        log.warn(">>> HTTP endpoint received AppointmentCancelledEvent: {}", event);
        listener.handleEvent(event);
    }

    @PostMapping("/appointment-status-changed")
    public void handleAppointmentStatusChanged(@Valid @RequestBody AppointmentStatusChangedEvent event) {
        log.warn(">>> HTTP endpoint received AppointmentStatusChangedEvent: {}", event);
        listener.handleEvent(event);
    }

    @PostMapping("/appointment-confirmation")
    public void handleAppointmentConfirmation(@Valid @RequestBody AppointmentConfirmationEvent event) {
        log.warn(">>> HTTP endpoint received AppointmentConfirmationEvent: {}", event);
        listener.handleEvent(event);
    }

}
