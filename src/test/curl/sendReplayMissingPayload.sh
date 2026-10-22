#!/bin/bash

echo "=== Testing replay with MISSING payload ==="

# This event type is valid, so the service will attempt to deserialize it.
TYPE="AppointmentCreatedEvent"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -u demo:demo \
  -X POST http://localhost:8080/events/replay/$TYPE)

echo "HTTP Status: $STATUS"

