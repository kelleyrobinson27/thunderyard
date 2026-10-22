#!/bin/bash

# Load shared event data
source ./commonEventData

# Require eventType parameter
EVENT_TYPE="$1"

if [ -z "$EVENT_TYPE" ]; then
  echo "Usage: ./changeAppointmentEvent.sh <CREATED|UPDATED|CANCELLED>"
  exit 1
fi

curl -v -u demo:demo \
  -X POST http://localhost:8080/events/appointment-changed \
  -H "Content-Type: application/json" \
  -d "{
        \"eventId\": \"$EVENT_ID\",
        \"appointmentId\": \"$APPOINTMENT_ID\",
        \"patientId\": \"$PATIENT_ID\",
        \"eventType\": \"$EVENT_TYPE\"
      }"

