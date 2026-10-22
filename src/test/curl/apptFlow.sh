#!/bin/bash
#
# What it hits:
#   Manual demo: create (queue SMS) → change UPDATED (queue SMS) → typed cancel
#   (SUPPRESSED). Same appointmentId. Pause before each part so you can clear logs.
#
# Kafka required for create/change. Not part of runAll.sh.
#
set -u
cd "$(dirname "$0")"

AUTH=(-u demo:demo)
BASE="http://localhost:8080"

new_uuid() {
  if command -v uuidgen >/dev/null 2>&1; then
    uuidgen | tr '[:upper:]' '[:lower:]' | tr -d '\r'
    return
  fi
  python -c "import uuid; print(uuid.uuid4())" 2>/dev/null && return
  python3 -c "import uuid; print(uuid.uuid4())" 2>/dev/null && return
  echo "00000000-0000-4000-8000-$(date +%s | tr -d '\r')"
}

pause() {
  echo
  printf 'Clear the logs, then press <enter> '
  read -r _
  echo
}

lookup() {
  local event_id="$1"
  local expect="$2"
  echo "GET $BASE/notifications?eventId=$event_id"
  LOOKUP=$(curl -s --max-time 20 -w "\nHTTPCODE:%{http_code}" \
    "${AUTH[@]}" "$BASE/notifications?eventId=$event_id" || true)
  LOOKUP_CODE=$(printf '%s' "$LOOKUP" | tr -d '\r' | grep '^HTTPCODE:' | tail -1 | cut -d: -f2)
  LOOKUP_BODY=$(printf '%s' "$LOOKUP" | sed '$d' | tr -d '\r')
  echo "HTTP $LOOKUP_CODE"
  echo "$LOOKUP_BODY"
  echo
  echo "'status' in the above JSON should be $expect"
  echo
  echo "Prove it in the DB (psql, database thunderyard):"
  echo "SELECT notification_id, status, trace_id FROM notifications WHERE event_id = '$event_id';"
  echo
}

APPT=$(new_uuid)
echo "appointmentId=$APPT  (same appointment for create, change, cancel)"
echo "Kafka must be up; app running. Watch IntelliJ after each Enter."

##############################################################################
pause

EVENT=$(new_uuid)
PATIENT=$(new_uuid)
echo "=== 1. CREATE ==="
echo "POST $BASE/events/appointment-created"
echo "eventId=$EVENT"
echo "appointmentId=$APPT"
echo "Watch logs for: Queued SMS job ... eventId=$EVENT"
echo "Then watch for: Sending SMS correlationId=..."
echo "The correlationIds from the lines above will match."
echo

CODE=$(curl -s --max-time 20 -o /dev/null -w "%{http_code}" \
  "${AUTH[@]}" \
  -X POST "$BASE/events/appointment-created" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$EVENT\",\"appointmentId\":\"$APPT\",\"patientId\":\"$PATIENT\",\"timestamp\":\"2026-10-22T18:00:00Z\",\"source\":\"apptFlow\"}" \
  || true)
CODE=$(printf '%s' "$CODE" | tr -d '\r')
echo "HTTP $CODE"
echo
if [ "$CODE" != "200" ]; then
  echo "Expected HTTP 200. If 500, start Kafka (go.bat / ./go.sh) and restart the app."
  exit 1
fi
sleep 1
lookup "$EVENT" "'SENT' (went from QUEUED then quickly to SENT)."

##############################################################################
pause

EVENT=$(new_uuid)
PATIENT=$(new_uuid)
echo "=== 2. CHANGE ==="
echo "POST $BASE/events/appointment-changed  eventType=UPDATED"
echo "appointmentId=$APPT"
echo "eventId=$EVENT  (new event; Kafka key is appointmentId)"
echo "Watch logs for: Queued SMS job ... eventId=$EVENT"
echo "Then watch for: Sending SMS correlationId=..."
echo "The correlationIds from the lines above will match."
echo

CODE=$(curl -s --max-time 20 -o /dev/null -w "%{http_code}" \
  "${AUTH[@]}" \
  -X POST "$BASE/events/appointment-changed" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$EVENT\",\"appointmentId\":\"$APPT\",\"patientId\":\"$PATIENT\",\"eventType\":\"UPDATED\",\"timestamp\":\"2026-10-22T18:00:00Z\",\"source\":\"apptFlow\"}" \
  || true)
CODE=$(printf '%s' "$CODE" | tr -d '\r')
echo "HTTP $CODE"
echo
if [ "$CODE" != "200" ]; then
  echo "Expected HTTP 200. If 500, start Kafka (go.bat / ./go.sh) and restart the app."
  exit 1
fi
sleep 1
lookup "$EVENT" "'SENT' (went from QUEUED then quickly to SENT)."

##############################################################################
pause

EVENT=$(new_uuid)
PATIENT=$(new_uuid)
echo "=== 3. CANCEL ==="
echo "POST $BASE/events/appointment-cancelled"
echo "appointmentId=$APPT"
echo "eventId=$EVENT"
echo "Watch logs for: HTTP endpoint received AppointmentCancelledEvent ... eventId=$EVENT"
echo "Watch also for: EAS stub canNotify=true for appointment $APPT"
echo

CODE=$(curl -s --max-time 20 -o /dev/null -w "%{http_code}" \
  "${AUTH[@]}" \
  -X POST "$BASE/events/appointment-cancelled" \
  -H "Content-Type: application/json" \
  -d "{\"eventId\":\"$EVENT\",\"appointmentId\":\"$APPT\",\"patientId\":\"$PATIENT\",\"reason\":\"PATIENT_REQUEST\",\"timestamp\":\"2026-10-22T18:00:00Z\",\"source\":\"apptFlow\"}" \
  || true)
CODE=$(printf '%s' "$CODE" | tr -d '\r')
echo "HTTP $CODE"
echo
if [ "$CODE" != "200" ]; then
  echo "Expected HTTP 200."
  exit 1
fi
sleep 1
lookup "$EVENT" "'SUPPRESSED' (no SMS job)."

exit 0
