#!/bin/bash
#
# What it hits:
#   Typed events through the notify pipeline: reminder / created / status /
#   confirmation queue SMS; appointment-cancelled is SUPPRESSED; duplicate
#   reminder eventId does not queue twice.
#
# Needs Kafka (queue path). eas.stub=true.

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
  if [ "$code" = "$expected" ]; then
    echo "[PASS] $name - HTTP $code"
    PASS=$((PASS + 1))
  else
    echo "[FAIL] $name - HTTP $code (expected $expected)"
    FAIL=$((FAIL + 1))
  fi
  echo
}

post() {
  local name="$1"
  local path="$2"
  local json="$3"
  echo "=== $name ==="
  local code
  code=$(curl -s --max-time 20 -o /dev/null -w "%{http_code}" \
    "${AUTH[@]}" -X POST "$BASE$path" \
    -H "Content-Type: application/json" \
    -d "$json" || true)
  record "$name" "$code" 200
}

echo "ThunderYard typed-event pipeline suite  ($BASE)"
echo "Kafka must be listening on localhost:9092"
echo

REMINDER=$(new_uuid)
R_APPT=$(new_uuid)
R_PATIENT=$(new_uuid)
CREATED=$(new_uuid)
C_APPT=$(new_uuid)
C_PATIENT=$(new_uuid)
CANCELLED=$(new_uuid)
X_APPT=$(new_uuid)
X_PATIENT=$(new_uuid)
STATUS=$(new_uuid)
S_APPT=$(new_uuid)
S_PATIENT=$(new_uuid)
CONFIRM=$(new_uuid)
F_APPT=$(new_uuid)
F_PATIENT=$(new_uuid)

post "POST appointment-reminder queues SMS" "/events/appointment-reminder" \
  "{\"eventId\":\"$REMINDER\",\"appointmentId\":\"$R_APPT\",\"patientId\":\"$R_PATIENT\",\"reminderType\":\"UPCOMING\",\"timestamp\":\"$TS\",\"source\":\"runAll8Typed\"}"

post "POST duplicate reminder eventId" "/events/appointment-reminder" \
  "{\"eventId\":\"$REMINDER\",\"appointmentId\":\"$R_APPT\",\"patientId\":\"$R_PATIENT\",\"reminderType\":\"UPCOMING\",\"timestamp\":\"$TS\",\"source\":\"runAll8Typed\"}"

post "POST appointment-created queues SMS" "/events/appointment-created" \
  "{\"eventId\":\"$CREATED\",\"appointmentId\":\"$C_APPT\",\"patientId\":\"$C_PATIENT\",\"timestamp\":\"$TS\",\"source\":\"runAll8Typed\"}"

post "POST appointment-cancelled suppressed" "/events/appointment-cancelled" \
  "{\"eventId\":\"$CANCELLED\",\"appointmentId\":\"$X_APPT\",\"patientId\":\"$X_PATIENT\",\"reason\":\"PATIENT_REQUEST\",\"timestamp\":\"$TS\",\"source\":\"runAll8Typed\"}"

post "POST appointment-status-changed queues SMS" "/events/appointment-status-changed" \
  "{\"eventId\":\"$STATUS\",\"appointmentId\":\"$S_APPT\",\"patientId\":\"$S_PATIENT\",\"fromStatus\":\"SCHEDULED\",\"toStatus\":\"IN_PROGRESS\",\"timestamp\":\"$TS\",\"source\":\"runAll8Typed\"}"

post "POST appointment-confirmation queues SMS" "/events/appointment-confirmation" \
  "{\"eventId\":\"$CONFIRM\",\"appointmentId\":\"$F_APPT\",\"patientId\":\"$F_PATIENT\",\"confirmationType\":\"PATIENT_CONFIRMED\",\"timestamp\":\"$TS\",\"source\":\"runAll8Typed\"}"

echo "========"
echo "PASS=$PASS  FAIL=$FAIL"
echo "========"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
