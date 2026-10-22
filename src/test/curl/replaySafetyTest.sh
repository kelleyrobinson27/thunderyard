#!/bin/bash

COUNT=51
echo "=== Creating $COUNT fake events to trigger safety limit ==="

for i in $(seq 1 $COUNT)
do
  curl -s -X POST http://localhost:8080/events/appointment-changed \
    -H "Content-Type: application/json" \
    -d "{
      \"eventId\": \"00000000-0000-0000-0000-$(printf '%012d' $i)\",
      \"appointmentId\": \"11111111-1111-1111-1111-111111111111\",
      \"patientId\": \"22222222-2222-2222-2222-222222222222\",
      \"eventType\": \"UPDATED\",
      \"timestamp\": \"2026-10-21T17:00:00Z\",
      \"source\": \"safety-test\"
    }" > /dev/null
done

echo "=== Triggering replay ==="

# Capture body + status code
HTTP_CODE=$(curl -s -o replay_response.txt -w "%{http_code}" \
  -X POST http://localhost:8080/events/replay/all)

echo "HTTP Status: $HTTP_CODE"
echo "=== Response Body ==="
cat replay_response.txt
echo
