#!/bin/bash
#
# What it hits:
#   EAS opt-in vs reserved opt-out appointmentId.
#
# eas.stub=true in application-local.yml.
# Random appointmentId -> canNotify=true, HTTP 200, look for Queued SMS job.
# Reserved opt-out id  -> canNotify=false, HTTP 200, SUPPRESSED, no SMS job.
# OPT_OUT_APPOINTMENT_ID = 00000000-0000-0000-0000-00000000ffff

set -u
cd "$(dirname "$0")"

AUTH=(-u demo:demo)
BASE="http://localhost:8080"
OPT_OUT="00000000-0000-0000-0000-00000000ffff"
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
  echo "appointmentId=$appointment_id"
  local code
  code=$(curl -s --max-time 20 -o /tmp/thunderyard-runAll3Eas-body.txt -w "%{http_code}" \
    "${AUTH[@]}" \
    -X POST "$BASE/events/appointment-changed" \
    -H "Content-Type: application/json" \
    -d "{\"eventId\":\"$event_id\",\"appointmentId\":\"$appointment_id\",\"patientId\":\"$patient_id\",\"eventType\":\"UPDATED\",\"timestamp\":\"2026-10-21T17:00:00Z\",\"source\":\"runAll3Eas\"}" \
    || true)
  record "$name" "$code" 200
}

echo "ThunderYard EAS preference suite  ($BASE)"
echo "Restart the app after the stub opt-out change."
echo

OPT_IN_EVENT=$(new_uuid)
OPT_IN_APPT=$(new_uuid)
OPT_IN_PATIENT=$(new_uuid)
OPT_OUT_EVENT=$(new_uuid)
OPT_OUT_PATIENT=$(new_uuid)

post_changed "UPDATED opt-in random appointmentId" "$OPT_IN_EVENT" "$OPT_IN_APPT" "$OPT_IN_PATIENT"
post_changed "UPDATED opt-out reserved appointmentId" "$OPT_OUT_EVENT" "$OPT_OUT" "$OPT_OUT_PATIENT"

echo "Log checks (not asserted by curl):"
echo "  opt-in:  EAS stub canNotify=true  and Queued SMS job corr-$OPT_IN_EVENT"
echo "  opt-out: EAS stub canNotify=false and Suppressing eventId=$OPT_OUT_EVENT"
echo "           no Queued SMS job for corr-$OPT_OUT_EVENT"
echo "========"
echo "PASS=$PASS  FAIL=$FAIL"
echo "========"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
