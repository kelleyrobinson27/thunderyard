#!/bin/bash

curl -v -u demo:demo \
  -X POST http://localhost:8080/events/appointment-reminder \
  -H "Content-Type: application/json" \
  -d '{
        "eventId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        "reminderType": "UPCOMING"
      }'

