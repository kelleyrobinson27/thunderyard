#!/bin/bash
#
# What it hits:
#   W3C traceparent on POST appointment-changed CANCELLED (no Kafka):
#   inbound traceparent echoed as traceresponse and X-Trace-Id;
#   missing header still generates a 32-hex trace id.
#
# Kafka is not required.

set -u
cd "$(dirname "$0")"

AUTH=(-u demo:demo)
BASE="http://localhost:8080"
PASS=0
FAIL=0
TRACEPARENT="00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
TRACE_ID="4bf92f3577b34da6a3ce929d0e0e4736"

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

http_code_from_headers() {
  printf '%s' "$1" | tr -d '\r' | grep -E '^HTTP/' | tail -1 | awk '{print $2}'
}

post_cancelled() {
  local extra_header="$1"
  EVENT=$(new_uuid)
  APPT=$(new_uuid)
  PATIENT=$(new_uuid)
  if [ -n "$extra_header" ]; then
    curl -s --max-time 20 -D - -o /dev/null \
      "${AUTH[@]}" \
      -X POST "$BASE/events/appointment-changed" \
      -H "Content-Type: application/json" \
      -H "$extra_header" \
      -d "{\"eventId\":\"$EVENT\",\"appointmentId\":\"$APPT\",\"patientId\":\"$PATIENT\",\"eventType\":\"CANCELLED\",\"timestamp\":\"2026-10-21T17:00:00Z\",\"source\":\"runAll10Trace\"}"
  else
    curl -s --max-time 20 -D - -o /dev/null \
      "${AUTH[@]}" \
      -X POST "$BASE/events/appointment-changed" \
      -H "Content-Type: application/json" \
      -d "{\"eventId\":\"$EVENT\",\"appointmentId\":\"$APPT\",\"patientId\":\"$PATIENT\",\"eventType\":\"CANCELLED\",\"timestamp\":\"2026-10-21T17:00:00Z\",\"source\":\"runAll10Trace\"}"
  fi
}

echo "ThunderYard W3C traceparent suite  ($BASE)"
echo

echo "=== POST with traceparent ==="
HEADERS=$(post_cancelled "traceparent: $TRACEPARENT" || true)
CODE=$(http_code_from_headers "$HEADERS")
record "POST inbound traceparent" "$CODE" 200
XTRACE=$(header_value "$HEADERS" "X-Trace-Id")
TRESP=$(header_value "$HEADERS" "traceresponse")
if [ "$XTRACE" = "$TRACE_ID" ]; then
  check "X-Trace-Id echoes inbound trace id" yes
else
  echo "  got X-Trace-Id=$XTRACE"
  check "X-Trace-Id echoes inbound trace id" no
fi
if [ "$TRESP" = "$TRACEPARENT" ]; then
  check "traceresponse echoes traceparent" yes
else
  echo "  got traceresponse=$TRESP"
  check "traceresponse echoes traceparent" no
fi

echo "=== POST without traceparent (generated) ==="
HEADERS=$(post_cancelled "" || true)
CODE=$(http_code_from_headers "$HEADERS")
record "POST generated trace" "$CODE" 200
GEN=$(header_value "$HEADERS" "X-Trace-Id")
if printf '%s' "$GEN" | grep -Eq '^[0-9a-f]{32}$'; then
  check "generated X-Trace-Id is 32 hex chars" yes
else
  echo "  got X-Trace-Id=$GEN"
  check "generated X-Trace-Id is 32 hex chars" no
fi

echo "========"
echo "PASS=$PASS  FAIL=$FAIL"
echo "========"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
