#!/bin/bash

echo "=== Sending invalid JSON to test error handling ==="

curl -X POST http://localhost:8080/events/changed \
  -H "Content-Type: application/json" \
  -d "{ invalid json }"
echo

