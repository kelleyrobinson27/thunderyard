#!/bin/bash

run_test() {
  local name=$1
  shift

  echo "=== Sending $name ==="

  # Build command array from all remaining arguments
  local cmd=("$@")

  # Execute command with all arguments preserved
  RESPONSE=$("${cmd[@]}" 2>&1)

  # Extract HTTP status code
  HTTP_CODE=$(echo "$RESPONSE" | grep "< HTTP/" | awk '{print $3}')

  if [ "$HTTP_CODE" == "200" ]; then
    echo "[PASS] $name returned HTTP 200"
  else
    echo "[FAIL] $name returned HTTP $HTTP_CODE"
  fi

  echo
}

run_test "AppointmentChangedEvent" ./sendAppointmentChangedEvent.sh CREATED
run_test "AppointmentReminderEvent" ./sendAppointmentReminderEvent.sh
run_test "AppointmentCreatedEvent" ./sendAppointmentCreatedEvent.sh
run_test "AppointmentCancelledEvent" ./sendAppointmentCancelledEvent.sh
run_test "AppointmentStatusChangedEvent" ./sendAppointmentStatusChangedEvent.sh
run_test "AppointmentConfirmationEvent" ./sendAppointmentConfirmationEvent.sh

echo "=== All events processed ==="
