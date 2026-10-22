#!/bin/bash

TYPE=$1

if [ -z "$TYPE" ]; then
  echo "Usage: ./listEventsByType.sh AppointmentReminderEvent"
  exit 1
fi

echo "=== Listing events of type $TYPE ==="
curl -X GET "http://localhost:8080/events?type=$TYPE"
echo

