#!/bin/bash
#
# What it hits:
#   SMS success / retryable timeout / permanent failure, plus POST /sms/callback 404.
#
# Needs Kafka and eas.stub=true. HTTP 200 only means the event was accepted.
# Random patientId              -> send succeeds (SENT)
# 00000000-0000-0000-0000-00000000aaa1 -> retryable timeout, Kafka retry, then DLQ
# 00000000-0000-0000-0000-00000000aaa2 -> permanent false, FAILED, no requeue

set -u
cd "$(dirname "$0")"

AUTH=(-u demo:demo)
BASE="http://localhost:8080"
RETRY_PATIENT="00000000-0000-0000-0000-00000000aaa1"
PERM_PATIENT="00000000-0000-0000-0000-00000000aaa2"
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

  echo "=== $name ==="
  echo "eventId=$event_id"
  echo "patientId=$patient_id"
  local code
  code=$(curl -s --max-time 20 -o /tmp/thunderyard-runAll4Sms-body.txt -w "%{http_code}" \
    "${AUTH[@]}" \
    -X POST "$BASE/events/appointment-changed" \
    -H "Content-Type: application/json" \
    -d "{\"eventId\":\"$event_id\",\"appointmentId\":\"$appointment_id\",\"patientId\":\"$patient_id\",\"eventType\":\"UPDATED\",\"timestamp\":\"2026-10-21T17:00:00Z\",\"source\":\"runAll4Sms\"}" \
    || true)
  record "$name" "$code" 200
}

echo "ThunderYard SMS provider suite  ($BASE)"
echo "Kafka must be up. Restart the app after this slice."
echo

OK_EVENT=$(new_uuid)
OK_APPT=$(new_uuid)
OK_PATIENT=$(new_uuid)
RETRY_EVENT=$(new_uuid)
RETRY_APPT=$(new_uuid)
PERM_EVENT=$(new_uuid)
PERM_APPT=$(new_uuid)

post_changed "UPDATED success send" "$OK_EVENT" "$OK_APPT" "$OK_PATIENT"
post_changed "UPDATED retryable SMS timeout" "$RETRY_EVENT" "$RETRY_APPT" "$RETRY_PATIENT"
post_changed "UPDATED permanent SMS failure" "$PERM_EVENT" "$PERM_APPT" "$PERM_PATIENT"

echo "=== POST /sms/callback unknown notificationId ==="
UNKNOWN_NID=$(new_uuid)
CODE=$(curl -s --max-time 20 -o /dev/null -w "%{http_code}" \
  "${AUTH[@]}" \
  -X POST "$BASE/sms/callback" \
  -H "Content-Type: application/json" \
  -d "{\"notificationId\":\"$UNKNOWN_NID\",\"correlationId\":\"corr-missing\",\"status\":\"DELIVERED\",\"providerCode\":\"OK\"}" \
  || true)
record "callback unknown notification" "$CODE" 404

echo "Log checks (not asserted by curl):"
echo "  success: Sending SMS correlationId=corr-$OK_EVENT"
echo "  retry:   SMS provider timeout / Retryable SMS failure corr-$RETRY_EVENT then DLQ"
echo "  perm:    FAILED Provider returned failure for corr-$PERM_EVENT, no DLQ retry loop"
echo "========"
echo "PASS=$PASS  FAIL=$FAIL"
echo "========"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
