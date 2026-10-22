#!/bin/bash

echo "=== Testing replay of unknown event type ==="
curl -X POST http://localhost:8080/events/replay/UnknownEventType
echo

