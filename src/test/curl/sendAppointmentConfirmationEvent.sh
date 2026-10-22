#!/bin/bash

curl -v -u demo:demo \
  -X POST http://localhost:8080/events/appointment-confirmation \
  -H "Content-Type: application/json" \
  -d '{
        "eventId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        "appointmentId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
        "patientId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
        "confirmationType": "PATIENT_CONFIRMED",
        "timestamp": "2026-10-21T17:00:00Z",
        "source": "curl-test"
      }'

