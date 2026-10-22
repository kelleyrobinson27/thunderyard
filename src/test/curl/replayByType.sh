#!/bin/bash

TYPE=$1

if [ -z "$TYPE" ]; then
  echo "Usage: ./replayByType.sh AppointmentChangedEvent"
  exit 1
fi

echo "=== Replaying events of type $TYPE ==="
curl -X POST http://localhost:8080/events/replay/$TYPE
echo

