#!/bin/bash
#
# Notification counters on /actuator/prometheus.
# POSTs unique UPDATED events then scrapes:
#   notifications_queued_total, notifications_sent_total,
#   notifications_suppressed_total, notifications_duplicated_total
# Kafka should be up so queued can become sent.

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

metric_present() {
  local body="$1"
  local name="$2"
  if printf '%s' "$body" | grep -q "$name"; then
    echo "[PASS] prometheus contains $name"
    PASS=$((PASS + 1))
  else
    echo "[FAIL] prometheus missing $name"
    FAIL=$((FAIL + 1))
  fi
}

post_changed() {
  local event_id="$1"
  local appointment_id="$2"
  local patient_id="$3"
  local event_type="$4"
  curl -s --max-time 20 -o /dev/null -w "%{http_code}" \
    "${AUTH[@]}" \
    -X POST "$BASE/events/appointment-changed" \
    -H "Content-Type: application/json" \
    -d "{\"eventId\":\"$event_id\",\"appointmentId\":\"$appointment_id\",\"patientId\":\"$patient_id\",\"eventType\":\"$event_type\",\"timestamp\":\"2026-10-21T17:00:00Z\",\"source\":\"runAll5Metrics\"}"
}

echo "ThunderYard metrics suite  ($BASE)"
echo

EVENT=$(new_uuid)
APPT=$(new_uuid)
PATIENT=$(new_uuid)

echo "=== POST UPDATED (queued + sent) ==="
CODE=$(post_changed "$EVENT" "$APPT" "$PATIENT" UPDATED || true)
record "UPDATED" "$CODE" 200

echo "=== POST duplicate eventId (duplicated) ==="
CODE=$(post_changed "$EVENT" "$APPT" "$PATIENT" UPDATED || true)
record "duplicate UPDATED" "$CODE" 200

CANCEL_EVENT=$(new_uuid)
CANCEL_APPT=$(new_uuid)
CANCEL_PATIENT=$(new_uuid)
echo "=== POST CANCELLED (suppressed) ==="
CODE=$(post_changed "$CANCEL_EVENT" "$CANCEL_APPT" "$CANCEL_PATIENT" CANCELLED || true)
record "CANCELLED" "$CODE" 200

sleep 2

echo "=== GET /actuator/prometheus ==="
PROM=$(curl -s --max-time 20 -o /dev/null -w "%{http_code}" "$BASE/actuator/prometheus" || true)
record "prometheus scrape" "$PROM" 200
BODY=$(curl -s --max-time 20 -H "Accept: text/plain" "$BASE/actuator/prometheus" | tr -d '\r' || true)
echo "scrape bytes=${#BODY}"

echo "--- notification counters ---"
printf '%s\n' "$BODY" | grep '^notifications_' || true
echo

metric_present "$BODY" "notifications_queued_total"
metric_present "$BODY" "notifications_sent_total"
metric_present "$BODY" "notifications_suppressed_total"
metric_present "$BODY" "notifications_duplicated_total"
metric_present "$BODY" "notifications_failed_total"
metric_present "$BODY" "notifications_retried_total"
metric_present "$BODY" "notifications_delivered_total"

echo "========"
echo "PASS=$PASS  FAIL=$FAIL"
echo "========"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
