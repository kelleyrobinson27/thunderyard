#!/bin/bash

curl -v -u demo:demo \
  -X POST http://localhost:8080/events/appointment-reminder \
  -H "Content-Type: application/json" \
  -d '{
        "eventId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        "appointmentId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
        "patientId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
        "reminderType": "NOT_A_REAL_TYPE"
      }'

