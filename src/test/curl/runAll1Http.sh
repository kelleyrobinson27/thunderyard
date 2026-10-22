#!/bin/bash
#
# What it hits:
#   Every event POST, validation 4xx, invalid path, list (all / type / date / count),
#   replay-by-type, unknown type, replay-all (200, or 413 if count > 50).
#
# Kafka is not required. Appointment-changed uses CANCELLED so SMS is not queued.
# CREATED/UPDATED + SMS are covered by runAll2Kafka.sh / runAll4Sms.sh.

set -u
cd "$(dirname "$0")"

AUTH=(-u demo:demo)
BASE="http://localhost:8080"
PASS=0
FAIL=0
TS="2026-10-21T17:00:00Z"

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
  if [ -z "$code" ]; then
    echo "[FAIL] $name - could not parse HTTP status (expected $expected)"
    FAIL=$((FAIL + 1))
  elif [[ "$code" =~ ^($expected)$ ]]; then
    echo "[PASS] $name - HTTP $code"
    PASS=$((PASS + 1))
  else
    echo "[FAIL] $name - HTTP $code (expected $expected)"
    FAIL=$((FAIL + 1))
  fi
  echo
}

run_curl() {
  local name="$1"
  local expected="$2"
  shift 2
  echo "=== $name ==="
  local code
  code=$(curl -s --max-time 20 -o /dev/null -w "%{http_code}" "$@" || true)
  record "$name" "$code" "$expected"
}

event_count() {
  local raw
  raw=$(curl -s --max-time 20 "${AUTH[@]}" "$1" || true)
  printf '%s' "$raw" | tr -d '\r[:space:]'
}

expected_replay_status() {
  local count
  count=$(printf '%s' "$1" | tr -d '\r[:space:]')
  if [ -n "$count" ] && [ "$count" -gt 50 ] 2>/dev/null; then
    printf '413'
  else
    printf '200'
  fi
}

run_replay() {
  local name="$1"
  local count="$2"
  shift 2
  local expected
  expected=$(expected_replay_status "$count")
  echo "=== $name ($count stored, expect HTTP $expected) ==="
  local code
  code=$(curl -s --max-time 20 -o /dev/null -w "%{http_code}" "$@" || true)
  if [ "$expected" = "413" ] && [ "$(printf '%s' "$code" | tr -d '\r')" = "413" ]; then
    echo "[PASS] $name - HTTP 413 (replay safety cap, $count events)"
    PASS=$((PASS + 1))
    echo
  else
    record "$name" "$code" "$expected"
  fi
}

ids() {
  EVENT=$(new_uuid)
  APPT=$(new_uuid)
  PATIENT=$(new_uuid)
}

echo "ThunderYard HTTP suite  ($BASE)"
echo

# --- Happy-path ingest (unique ids; no SMS queue) ---
ids
run_curl "POST appointment-changed CANCELLED" 200 \
  "${AUTH[@]}" -X POST "$BASE/events/appointment-changed" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$EVENT\",\"appointmentId\":\"$APPT\",\"patientId\":\"$PATIENT\",\"eventType\":\"CANCELLED\",\"timestamp\":\"$TS\",\"source\":\"runAll1Http\"}"

ids
run_curl "POST appointment-created" 200 \
  "${AUTH[@]}" -X POST "$BASE/events/appointment-created" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$EVENT\",\"appointmentId\":\"$APPT\",\"patientId\":\"$PATIENT\",\"timestamp\":\"$TS\",\"source\":\"runAll1Http\"}"

ids
run_curl "POST appointment-cancelled" 200 \
  "${AUTH[@]}" -X POST "$BASE/events/appointment-cancelled" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$EVENT\",\"appointmentId\":\"$APPT\",\"patientId\":\"$PATIENT\",\"reason\":\"PATIENT_REQUEST\",\"timestamp\":\"$TS\",\"source\":\"runAll1Http\"}"

ids
run_curl "POST appointment-status-changed" 200 \
  "${AUTH[@]}" -X POST "$BASE/events/appointment-status-changed" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$EVENT\",\"appointmentId\":\"$APPT\",\"patientId\":\"$PATIENT\",\"fromStatus\":\"SCHEDULED\",\"toStatus\":\"IN_PROGRESS\",\"timestamp\":\"$TS\",\"source\":\"runAll1Http\"}"

ids
run_curl "POST appointment-confirmation" 200 \
  "${AUTH[@]}" -X POST "$BASE/events/appointment-confirmation" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$EVENT\",\"appointmentId\":\"$APPT\",\"patientId\":\"$PATIENT\",\"confirmationType\":\"PATIENT_CONFIRMED\",\"timestamp\":\"$TS\",\"source\":\"runAll1Http\"}"

ids
run_curl "POST appointment-reminder" 200 \
  "${AUTH[@]}" -X POST "$BASE/events/appointment-reminder" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$EVENT\",\"appointmentId\":\"$APPT\",\"patientId\":\"$PATIENT\",\"reminderType\":\"UPCOMING\",\"timestamp\":\"2026-10-21T16:00:00Z\",\"source\":\"runAll1Http\"}"

ids
run_curl "POST appointment-reminder DAY_OF" 200 \
  "${AUTH[@]}" -X POST "$BASE/events/appointment-reminder" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$EVENT\",\"appointmentId\":\"$APPT\",\"patientId\":\"$PATIENT\",\"reminderType\":\"DAY_OF\",\"timestamp\":\"$TS\",\"source\":\"runAll1Http\"}"

# --- Validation ---
BAD_EVENT=$(new_uuid)
run_curl "POST appointment-changed missing fields" 400 \
  "${AUTH[@]}" -X POST "$BASE/events/appointment-changed" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$BAD_EVENT\",\"eventType\":\"CREATED\"}"

ids
run_curl "POST appointment-changed bad eventType" 400 \
  "${AUTH[@]}" -X POST "$BASE/events/appointment-changed" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$EVENT\",\"appointmentId\":\"$APPT\",\"patientId\":\"$PATIENT\",\"eventType\":\"NOT_A_REAL_TYPE\"}"

run_curl "POST appointment-changed wrong Content-Type" "415|400" \
  "${AUTH[@]}" -X POST "$BASE/events/appointment-changed" \
  -H "Content-Type: text/plain" \
  -d '{"eventId":"11111111-1111-1111-1111-111111111111","appointmentId":"22222222-2222-2222-2222-222222222222","patientId":"33333333-3333-3333-3333-333333333333","eventType":"CREATED"}'

BAD_EVENT=$(new_uuid)
run_curl "POST appointment-reminder missing fields" 400 \
  "${AUTH[@]}" -X POST "$BASE/events/appointment-reminder" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$BAD_EVENT\",\"reminderType\":\"UPCOMING\"}"

ids
run_curl "POST appointment-reminder bad reminderType" 400 \
  "${AUTH[@]}" -X POST "$BASE/events/appointment-reminder" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$EVENT\",\"appointmentId\":\"$APPT\",\"patientId\":\"$PATIENT\",\"reminderType\":\"NOT_A_REAL_TYPE\"}"

run_curl "POST /events/changed invalid JSON no auth" "401|404|400" \
  -X POST "$BASE/events/changed" \
  -H "Content-Type: application/json" \
  -d "{ invalid json }"

# --- Reads ---
run_curl "GET /events" 200 \
  "${AUTH[@]}" "$BASE/events"

run_curl "GET /events/count" 200 \
  "${AUTH[@]}" "$BASE/events/count"

run_curl "GET /events date range" 200 \
  "${AUTH[@]}" "$BASE/events?from=2026-10-21T00:00:00Z&to=2026-10-22T00:00:00Z"

for TYPE in \
  AppointmentChangedEvent \
  AppointmentCreatedEvent \
  AppointmentCancelledEvent \
  AppointmentReminderEvent \
  AppointmentStatusChangedEvent \
  AppointmentConfirmationEvent
do
  run_curl "GET /events?type=$TYPE" 200 \
    "${AUTH[@]}" "$BASE/events?type=$TYPE"
done

# --- Replay ---
for TYPE in \
  AppointmentChangedEvent \
  AppointmentCreatedEvent \
  AppointmentCancelledEvent \
  AppointmentReminderEvent \
  AppointmentStatusChangedEvent \
  AppointmentConfirmationEvent
do
  TYPE_COUNT=$(event_count "$BASE/events/count?type=$TYPE")
  run_replay "POST /events/replay/$TYPE" "$TYPE_COUNT" \
    "${AUTH[@]}" -X POST "$BASE/events/replay/$TYPE"
done

run_curl "POST /events/replay/unknown type" 400 \
  "${AUTH[@]}" -X POST "$BASE/events/replay/TotallyFakeEventType"

ALL_COUNT=$(event_count "$BASE/events/count")
run_replay "POST /events/replay/all" "$ALL_COUNT" \
  "${AUTH[@]}" -X POST "$BASE/events/replay/all"

echo "========"
echo "PASS=$PASS  FAIL=$FAIL"
echo "========"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
