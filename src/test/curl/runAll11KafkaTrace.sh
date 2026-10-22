#!/bin/bash
#
# What it hits:
#   W3C traceparent on POST appointment-changed UPDATED (needs Kafka):
#   inbound trace id is stored on the notification row and GET /notifications
#   returns it — same id the SmsJob carries onto sms-jobs for SmsWorker MDC.
#
# Needs: app on :8080 and Kafka on :9092.

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

EVENT=$(new_uuid)
APPT=$(new_uuid)
PATIENT=$(new_uuid)

echo "ThunderYard Kafka W3C trace suite  ($BASE)"
echo "Kafka must be listening on localhost:9092"
echo "eventId=$EVENT"
echo

echo "=== POST UPDATED with traceparent ==="
HEADERS=$(curl -s --max-time 20 -D - -o /dev/null \
  "${AUTH[@]}" \
  -X POST "$BASE/events/appointment-changed" \
  -H "Content-Type: application/json" \
  -H "traceparent: $TRACEPARENT" \
  -d "{\"eventId\":\"$EVENT\",\"appointmentId\":\"$APPT\",\"patientId\":\"$PATIENT\",\"eventType\":\"UPDATED\",\"timestamp\":\"2026-10-21T17:00:00Z\",\"source\":\"runAll11KafkaTrace\"}" \
  || true)
CODE=$(http_code_from_headers "$HEADERS")
record "POST inbound traceparent queues SMS" "$CODE" 200

XTRACE=$(header_value "$HEADERS" "X-Trace-Id")
if [ "$XTRACE" = "$TRACE_ID" ]; then
  check "X-Trace-Id echoes inbound trace id" yes
else
  echo "  got X-Trace-Id=$XTRACE"
  check "X-Trace-Id echoes inbound trace id" no
fi

echo "=== GET /notifications?eventId= (traceId persisted) ==="
LOOKUP=$(curl -s --max-time 20 -w "\nHTTPCODE:%{http_code}" \
  "${AUTH[@]}" "$BASE/notifications?eventId=$EVENT" || true)
LOOKUP_CODE=$(printf '%s' "$LOOKUP" | tr -d '\r' | grep '^HTTPCODE:' | tail -1 | cut -d: -f2)
LOOKUP_BODY=$(printf '%s' "$LOOKUP" | sed '$d' | tr -d '\r')
record "GET notification by eventId" "$LOOKUP_CODE" 200

GOT_TRACE=$(printf '%s' "$LOOKUP_BODY" | python -c "import json,sys; print(json.load(sys.stdin).get('traceId',''))" 2>/dev/null | tr -d '\r')
if [ "$GOT_TRACE" = "$TRACE_ID" ]; then
  check "notification.traceId matches inbound trace" yes
else
  echo "  got traceId=$GOT_TRACE body=$LOOKUP_BODY"
  check "notification.traceId matches inbound trace" no
fi

echo "========"
echo "PASS=$PASS  FAIL=$FAIL"
echo "========"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
