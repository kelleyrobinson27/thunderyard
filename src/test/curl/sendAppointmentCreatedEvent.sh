#!/bin/bash

curl -v -u demo:demo \
  -X POST http://localhost:8080/events/appointment-created \
  -H "Content-Type: application/json" \
  -d '{
        "eventId": "11111111-1111-1111-1111-111111111111",
        "appointmentId": "22222222-2222-2222-2222-222222222222",
        "patientId": "33333333-3333-3333-3333-333333333333",
        "timestamp": "2026-10-21T17:00:00Z",
        "source": "curl-test"
      }'

