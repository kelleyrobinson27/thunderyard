#!/bin/bash

echo "=== Testing replay deserialization and dispatch ==="

EVENT_TYPES=(
  "AppointmentChangedEvent"
  "AppointmentReminderEvent"
  "AppointmentCreatedEvent"
  "AppointmentCancelledEvent"
  "AppointmentStatusChangedEvent"
  "AppointmentConfirmationEvent"
)

for TYPE in "${EVENT_TYPES[@]}"
do
  echo ">>> Replaying $TYPE"
  curl -X POST http://localhost:8080/events/replay/$TYPE
  echo
done

