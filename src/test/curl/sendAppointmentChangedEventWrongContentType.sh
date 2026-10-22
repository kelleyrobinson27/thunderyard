#!/bin/bash

curl -v -u demo:demo \
  -X POST http://localhost:8080/events/appointment-changed \
  -H "Content-Type: text/plain" \
  -d '{
        "eventId": "11111111-1111-1111-1111-111111111111",
        "appointmentId": "22222222-2222-2222-2222-222222222222",
        "patientId": "33333333-3333-3333-3333-333333333333",
        "eventType": "CREATED"
      }'

