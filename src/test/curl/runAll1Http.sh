#!/bin/bash
#
# HTTP ingest / validation / list / replay. Kafka is not required.
# On http://localhost:8080. Prints PASS/FAIL per case and a summary.
#
# Skipped on purpose:
#   runAllEvents.sh / runAllAppointmentEvents.sh  — nested aggregators
#   replaySafetyTest.sh                            — inserts 51 rows and trips the replay cap
#   sendReplayMissingPayload.sh                    — just replays AppointmentCreatedEvent
#
# Replay/list scripts that omit Basic auth are invoked with curl -u demo:demo
# so results match a secured local app.
#
# Unknown-type replay expects 400 (type whitelist). Replay-all / replay-by-type
# expect 200, or 500 when more than 50 matching rows (safety cap).

set -u
cd "$(dirname "$0")"

AUTH=(-u demo:demo)
BASE="http://localhost:8080"

PASS=0
FAIL=0
SKIP=0

extract_http_code() {
  local out="$1"
  local code
  code=$(printf '%s\n' "$out" | grep -E '< HTTP/' | tail -1 | awk '{print $3}' | tr -d '\r')
  if [ -z "$code" ]; then
    code=$(printf '%s\n' "$out" | grep -E 'HTTP Status:' | tail -1 | awk '{print $3}' | tr -d '\r')
  fi
  if [ -z "$code" ]; then
    code=$(printf '%s\n' "$out" | grep -E '^[0-9]{3}$' | tail -1 | tr -d '\r')
  fi
  printf '%s' "$code"
}

code_ok() {
  local code="$1"
  local expected="$2"
  [[ "$code" =~ ^($expected)$ ]]
}

record() {
  local name="$1"
  local code="$2"
  local expected="$3"

  if [ -z "$code" ]; then
    echo "[FAIL] $name — could not parse HTTP status (expected $expected)"
    FAIL=$((FAIL + 1))
  elif code_ok "$code" "$expected"; then
    echo "[PASS] $name — HTTP $code"
    PASS=$((PASS + 1))
  else
    echo "[FAIL] $name — HTTP $code (expected $expected)"
    FAIL=$((FAIL + 1))
  fi
  echo
}

# Run a local *.sh that uses curl -v (or prints "HTTP Status:").
run_script() {
  local name="$1"
  local expected="$2"
  shift 2

  echo "=== $name ==="
  local output
  output=$("$@" 2>&1) || true
  local code
  code=$(extract_http_code "$output")
  record "$name" "$code" "$expected"
}

# Direct curl so we can require auth and read %{http_code} even when the
# matching script has no -v / -w.
run_curl() {
  local name="$1"
  local expected="$2"
  shift 2

  echo "=== $name ==="
  local code
  code=$(curl -s -o /tmp/thunderyard-curl-body.txt -w "%{http_code}" "$@" || true)
  code=$(printf '%s' "$code" | tr -d '\r')
  record "$name" "$code" "$expected"
}

# Integer count from GET /events/count (strip Cygwin/Windows CR).
event_count() {
  local url="$1"
  local raw
  raw=$(curl -s "${AUTH[@]}" "$url" || true)
  printf '%s' "$raw" | tr -d '\r[:space:]'
}

# Replay is 200 when the batch is <= 50, otherwise IllegalStateException → 500.
expected_replay_status() {
  local count
  count=$(printf '%s' "$1" | tr -d '\r[:space:]')
  if [ -n "$count" ] && [ "$count" -gt 50 ] 2>/dev/null; then
    printf '500'
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
  code=$(curl -s -o /tmp/thunderyard-curl-body.txt -w "%{http_code}" "$@" || true)
  code=$(printf '%s' "$code" | tr -d '\r')
  if [ "$expected" = "500" ] && [ "$code" = "500" ]; then
    echo "[PASS] $name — HTTP 500 (replay safety cap, $count events)"
    PASS=$((PASS + 1))
    echo
  else
    record "$name" "$code" "$expected"
  fi
}

echo "ThunderYard HTTP suite  ($BASE)"
echo

# --- Happy-path appointment events (expect 200) ---
run_script "sendAppointmentChangedEvent.sh CREATED" 200 \
  ./sendAppointmentChangedEvent.sh CREATED
run_script "sendAppointmentChangedEvent.sh UPDATED" 200 \
  ./sendAppointmentChangedEvent.sh UPDATED
run_script "sendAppointmentChangedEvent.sh CANCELLED" 200 \
  ./sendAppointmentChangedEvent.sh CANCELLED

run_script "changeAppointmentEvent.sh CREATED" 200 \
  ./changeAppointmentEvent.sh CREATED
run_script "changeAppointmentEvent.sh UPDATED" 200 \
  ./changeAppointmentEvent.sh UPDATED
run_script "changeAppointmentEvent.sh CANCELLED" 200 \
  ./changeAppointmentEvent.sh CANCELLED

run_script "sendAppointmentCreatedEvent.sh" 200 ./sendAppointmentCreatedEvent.sh
run_script "sendAppointmentCancelledEvent.sh" 200 ./sendAppointmentCancelledEvent.sh
run_script "sendAppointmentStatusChangedEvent.sh" 200 ./sendAppointmentStatusChangedEvent.sh
run_script "sendAppointmentConfirmationEvent.sh" 200 ./sendAppointmentConfirmationEvent.sh
run_script "sendAppointmentReminderEvent.sh" 200 ./sendAppointmentReminderEvent.sh
run_script "sendAppointmentRemindEvent.sh" 200 ./sendAppointmentRemindEvent.sh

# --- Validation / error cases (expect 4xx) ---
run_script "sendAppointmentChangedEventInvalid.sh (missing fields)" 400 \
  ./sendAppointmentChangedEventInvalid.sh
run_script "sendAppointmentChangedEventBadType.sh" 400 \
  ./sendAppointmentChangedEventBadType.sh
run_script "sendAppointmentChangedEventWrongContentType.sh" "415|400" \
  ./sendAppointmentChangedEventWrongContentType.sh
run_script "sendAppointmentReminderEventInvalid.sh (missing fields)" 400 \
  ./sendAppointmentReminderEventInvalid.sh
run_script "sendAppointmentReminderEventBadType.sh" 400 \
  ./sendAppointmentReminderEventBadType.sh

# Unknown path + invalid JSON, no auth → typically 401 from Spring Security
run_script "sendInvalidEvent.sh (POST /events/changed)" "401|404|400" \
  ./sendInvalidEvent.sh

# --- Reads (status only; do not download the full event dump) ---
run_curl "listEvents.sh" 200 \
  "${AUTH[@]}" -o /dev/null "$BASE/events"

for TYPE in \
  AppointmentChangedEvent \
  AppointmentCreatedEvent \
  AppointmentCancelledEvent \
  AppointmentReminderEvent \
  AppointmentStatusChangedEvent \
  AppointmentConfirmationEvent
do
  run_curl "listEventsByType.sh $TYPE" 200 \
    "${AUTH[@]}" -o /dev/null "$BASE/events?type=$TYPE"
done

# --- Replay (authenticated; child scripts omit -u and often 401) ---
for TYPE in \
  AppointmentChangedEvent \
  AppointmentCreatedEvent \
  AppointmentCancelledEvent \
  AppointmentReminderEvent \
  AppointmentStatusChangedEvent \
  AppointmentConfirmationEvent
do
  TYPE_COUNT=$(event_count "$BASE/events/count?type=$TYPE")
  run_replay "replayByType.sh $TYPE" "$TYPE_COUNT" \
    "${AUTH[@]}" -X POST "$BASE/events/replay/$TYPE"
done

# Whitelist rejects unknown types as IllegalArgumentException → HTTP 400
run_script "sendReplayUnknownType.sh" 400 ./sendReplayUnknownType.sh

ALL_COUNT=$(event_count "$BASE/events/count")
run_replay "replayAll.sh" "$ALL_COUNT" \
  "${AUTH[@]}" -X POST "$BASE/events/replay/all"

echo "Skipped:"
echo "  [SKIP] replaySafetyTest.sh — inserts 51 events and is meant to abort replay"
echo "  [SKIP] sendReplayMissingPayload.sh — same as replay-by-type AppointmentCreatedEvent"
echo "  [SKIP] runAllEvents.sh / runAllAppointmentEvents.sh — nested runners"
SKIP=$((SKIP + 3))
echo

echo "========"
echo "PASS=$PASS  FAIL=$FAIL  SKIP=$SKIP"
echo "========"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
