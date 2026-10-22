#!/bin/bash
#
# Correlation ID on HTTP: echo X-Correlation-Id and persist it on the event.
# Kafka is not required for these checks (POST still queues SMS if the broker is up).

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

check() {
  local name="$1"
  local ok="$2"
  if [ "$ok" = "yes" ]; then
    echo "[PASS] $name"
    PASS=$((PASS + 1))
  else
    echo "[FAIL] $name"
    FAIL=$((FAIL + 1))
  fi
  echo
}

header_value() {
  local headers="$1"
  local name="$2"
  printf '%s' "$headers" | tr -d '\r' | grep -i "^$name:" | tail -1 | awk -F': ' '{print $2}'
}

post_changed() {
  local event_id="$1"
  local appointment_id="$2"
  local patient_id="$3"
  local extra_header="$4"
  if [ -n "$extra_header" ]; then
    curl -s --max-time 20 -D - -o /dev/null \
      "${AUTH[@]}" \
      -X POST "$BASE/events/appointment-changed" \
      -H "Content-Type: application/json" \
      -H "$extra_header" \
      -d "{\"eventId\":\"$event_id\",\"appointmentId\":\"$appointment_id\",\"patientId\":\"$patient_id\",\"eventType\":\"UPDATED\",\"timestamp\":\"2026-10-21T17:00:00Z\",\"source\":\"runAll6Tracing\"}"
  else
    curl -s --max-time 20 -D - -o /dev/null \
      "${AUTH[@]}" \
      -X POST "$BASE/events/appointment-changed" \
      -H "Content-Type: application/json" \
      -d "{\"eventId\":\"$event_id\",\"appointmentId\":\"$appointment_id\",\"patientId\":\"$patient_id\",\"eventType\":\"UPDATED\",\"timestamp\":\"2026-10-21T17:00:00Z\",\"source\":\"runAll6Tracing\"}"
  fi
}

http_code_from_headers() {
  printf '%s' "$1" | tr -d '\r' | grep -E '^HTTP/' | tail -1 | awk '{print $2}'
}

echo "ThunderYard tracing suite  ($BASE)"
echo

CORR="corr-$(new_uuid)"
EVENT=$(new_uuid)
APPT=$(new_uuid)
PATIENT=$(new_uuid)

echo "=== POST with X-Correlation-Id ==="
HEADERS=$(post_changed "$EVENT" "$APPT" "$PATIENT" "X-Correlation-Id: $CORR" || true)
CODE=$(http_code_from_headers "$HEADERS")
record "POST inbound corr" "$CODE" 200
ECHO=$(header_value "$HEADERS" "X-Correlation-Id")
if [ "$ECHO" = "$CORR" ]; then
  check "response echoes $CORR" yes
else
  echo "  got X-Correlation-Id=$ECHO"
  check "response echoes $CORR" no
fi

BODY=$(curl -s --max-time 20 "${AUTH[@]}" "$BASE/events?correlationId=$CORR" | tr -d '\r' || true)
if printf '%s' "$BODY" | grep -q "$CORR" && printf '%s' "$BODY" | grep -q "$EVENT"; then
  check "GET /events?correlationId= stored the event" yes
else
  echo "  body=$BODY"
  check "GET /events?correlationId= stored the event" no
fi

echo "=== POST without header (generated id) ==="
EVENT2=$(new_uuid)
APPT2=$(new_uuid)
PATIENT2=$(new_uuid)
HEADERS=$(post_changed "$EVENT2" "$APPT2" "$PATIENT2" "" || true)
CODE=$(http_code_from_headers "$HEADERS")
record "POST generated corr" "$CODE" 200
GEN=$(header_value "$HEADERS" "X-Correlation-Id")
if [ -n "$GEN" ]; then
  check "response generated X-Correlation-Id" yes
else
  check "response generated X-Correlation-Id" no
fi

if [ -n "$GEN" ]; then
  BODY=$(curl -s --max-time 20 "${AUTH[@]}" "$BASE/events?correlationId=$GEN" | tr -d '\r' || true)
  if printf '%s' "$BODY" | grep -q "$GEN" && printf '%s' "$BODY" | grep -q "$EVENT2"; then
    check "GET stored generated correlationId" yes
  else
    echo "  body=$BODY"
    check "GET stored generated correlationId" no
  fi
fi

echo "========"
echo "PASS=$PASS  FAIL=$FAIL"
echo "========"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
