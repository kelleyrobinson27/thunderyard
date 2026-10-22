#!/bin/bash

run_event() {
  EVENT_TYPE="$1"
  echo "=== Sending $EVENT_TYPE event ==="

  # Capture both output and HTTP status code
  RESPONSE=$(./changeAppointmentEvent.sh "$EVENT_TYPE" 2>&1)
  STATUS=$(echo "$RESPONSE" | grep "< HTTP/" | awk '{print $3}')

  if [ "$STATUS" = "200" ]; then
    echo "[PASS] $EVENT_TYPE returned HTTP 200"
  else
    echo "[FAIL] $EVENT_TYPE returned HTTP $STATUS"
  fi

  echo
}

run_event "CREATED"
run_event "UPDATED"
run_event "CANCELLED"

echo "=== All events processed ==="

