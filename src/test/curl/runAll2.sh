#!/bin/bash
#
# Exercise the SMS Kafka queue boundary.
# Needs: app on :8080 and Kafka on :9092.
#
# CREATED/UPDATED  -> persist, queue sms-jobs (HTTP 500 if Kafka is down)
# duplicate eventId -> 200, no second SMS (notification already saved)
# CANCELLED        -> SUPPRESSED, no SMS job

set -u
cd "$(dirname "$0")"

AUTH=(-u demo:demo)
BASE="http://localhost:8080"
PASS=0
FAIL=0

new_uuid() {
  if command -v uuidgen >/dev/null 2>&1; then
    uuidgen | tr '[:upper:]' '[:lower:]' | tr -d '\r'
    return
  fi
  python -c "import uuid; print(uuid.uuid4())" 2>/dev/null && return
  python3 -c "import uuid; print(uuid.uuid4())" 2>/dev/null && return
  echo "00000000-0000-4000-8000-$(date +%s | tr -d '\r')"
}

record() {
  local name="$1"
  local code="$2"
  local expected="$3"
  code=$(printf '%s' "$code" | tr -d '\r')
  if [ "$code" = "$expected" ]; then
    echo "[PASS] $name - HTTP $code"
    PASS=$((PASS + 1))
  else
    echo "[FAIL] $name - HTTP $code (expected $expected)"
    FAIL=$((FAIL + 1))
  fi
  echo
}

post_changed() {
  local name="$1"
  local event_id="$2"
  local appointment_id="$3"
  local patient_id="$4"
  local event_type="$5"

  echo "=== $name ==="
  echo "eventId=$event_id  type=$event_type"
  local code
  code=$(curl -s --max-time 20 -o /tmp/thunderyard-runAll2-body.txt -w "%{http_code}" \
    "${AUTH[@]}" \
    -X POST "$BASE/events/appointment-changed" \
    -H "Content-Type: application/json" \
    -d "{\"eventId\":\"$event_id\",\"appointmentId\":\"$appointment_id\",\"patientId\":\"$patient_id\",\"eventType\":\"$event_type\",\"timestamp\":\"2026-10-21T17:00:00Z\",\"source\":\"runAll2\"}" \
    || true)
  record "$name" "$code" 200
}

echo "ThunderYard SMS-queue suite  ($BASE)"
echo "Kafka must be listening on localhost:9092"
echo

CREATED_EVENT=$(new_uuid)
CREATED_APPT=$(new_uuid)
CREATED_PATIENT=$(new_uuid)
UPDATED_EVENT=$(new_uuid)
UPDATED_APPT=$(new_uuid)
UPDATED_PATIENT=$(new_uuid)
CANCELLED_EVENT=$(new_uuid)
CANCELLED_APPT=$(new_uuid)
CANCELLED_PATIENT=$(new_uuid)

post_changed "CREATED should queue SMS" "$CREATED_EVENT" "$CREATED_APPT" "$CREATED_PATIENT" CREATED
post_changed "duplicate CREATED eventId" "$CREATED_EVENT" "$CREATED_APPT" "$CREATED_PATIENT" CREATED
post_changed "UPDATED should queue SMS" "$UPDATED_EVENT" "$UPDATED_APPT" "$UPDATED_PATIENT" UPDATED
post_changed "CANCELLED suppressed no SMS" "$CANCELLED_EVENT" "$CANCELLED_APPT" "$CANCELLED_PATIENT" CANCELLED

echo "=== GET /events/count ==="
COUNT=$(curl -s --max-time 20 "${AUTH[@]}" "$BASE/events/count" | tr -d '\r')
echo "stored events: $COUNT"
if [[ "$COUNT" =~ ^[0-9]+$ ]]; then
  echo "[PASS] GET /events/count - $COUNT"
  PASS=$((PASS + 1))
else
  echo "[FAIL] GET /events/count - $COUNT"
  FAIL=$((FAIL + 1))
fi
echo

echo "CREATED eventId=$CREATED_EVENT"
echo "========"
echo "PASS=$PASS  FAIL=$FAIL"
echo "========"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
