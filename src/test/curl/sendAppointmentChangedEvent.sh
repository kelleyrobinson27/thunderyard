
#!/bin/bash

TYPE=$1

curl -v -u demo:demo \
  -X POST http://localhost:8080/events/appointment-changed \
  -H "Content-Type: application/json" \
  -d "{
        \"eventId\": \"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",
        \"appointmentId\": \"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb\",
        \"patientId\": \"cccccccc-cccc-cccc-cccc-cccccccccccc\",
        \"eventType\": \"$TYPE\",
        \"timestamp\": \"2026-10-21T17:00:00Z\",
        \"source\": \"curl-test\"
      }"

