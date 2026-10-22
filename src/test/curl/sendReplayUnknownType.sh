#!/bin/bash

echo "=== Testing replay with UNKNOWN event type ==="

UNKNOWN_TYPE="TotallyFakeEventType"

STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -u demo:demo \
  -X POST http://localhost:8080/events/replay/$UNKNOWN_TYPE)

echo "HTTP Status: $STATUS"

