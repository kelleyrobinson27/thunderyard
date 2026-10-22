#!/bin/bash

# Load shared event data
source ./commonEventData

curl -v -u demo:demo \
  -X POST http://localhost:8080/events/appointment-reminder \
  -H "Content-Type: application/json" \
  -d "{
        \"eventId\": \"$EVENT_ID\",
        \"appointmentId\": \"$APPOINTMENT_ID\",
        \"patientId\": \"$PATIENT_ID\",
        \"reminderType\": \"UPCOMING\"
      }"

