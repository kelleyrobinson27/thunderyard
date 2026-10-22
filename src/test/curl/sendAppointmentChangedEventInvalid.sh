#!/bin/bash

curl -v -u demo:demo \
  -X POST http://localhost:8080/events/appointment-changed \
  -H "Content-Type: application/json" \
  -d '{
        "eventId": "11111111-1111-1111-1111-111111111111",
        "eventType": "CREATED"
      }'

