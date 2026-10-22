#!/bin/bash

echo "=== Sending invalid JSON to test error handling ==="

STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST http://localhost:8080/events/changed \
  -H "Content-Type: application/json" \
  -d "{ invalid json }")

echo "HTTP Status: $STATUS"

