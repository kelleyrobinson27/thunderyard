#!/bin/bash
#
# What it hits:
#   POST /sms/callback auth: no credentials 401, bad Bearer 401,
#   valid HS256 JWT unknown id 404, valid JWT + real notification 200 DELIVERED.
#   Basic still works for lookup GET /notifications?eventId= (and runAll4Sms).
#
# Kafka is required for the success path (queue an UPDATED event).

set -u
cd "$(dirname "$0")"

AUTH=(-u demo:demo)
BASE="http://localhost:8080"
SECRET="thunderyard-demo-hmac-secret-key-32"
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

hs256_jwt() {
  python - "$SECRET" <<'PY'
import base64, hashlib, hmac, sys
secret = sys.argv[1].encode()

def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode()

header = b64url(b'{"alg":"HS256","typ":"JWT"}')
payload = b64url(b'{"sub":"sms-provider"}')
sig = b64url(hmac.new(secret, f"{header}.{payload}".encode(), hashlib.sha256).digest())
print(f"{header}.{payload}.{sig}")
PY
}

callback_body() {
  local nid="$1"
  printf '{"notificationId":"%s","correlationId":"corr-jwt","status":"DELIVERED","providerCode":"OK"}' "$nid"
}

echo "ThunderYard JWT suite  ($BASE)"
echo

NID=$(new_uuid)
BODY=$(callback_body "$NID")

echo "=== POST /sms/callback no auth ==="
CODE=$(curl -s --max-time 20 -o /dev/null -w "%{http_code}" \
  -X POST "$BASE/sms/callback" \
  -H "Content-Type: application/json" \
  -d "$BODY" || true)
record "callback no auth" "$CODE" 401

echo "=== POST /sms/callback bad Bearer ==="
CODE=$(curl -s --max-time 20 -o /dev/null -w "%{http_code}" \
  -X POST "$BASE/sms/callback" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer not-a-jwt" \
  -d "$BODY" || true)
record "callback bad JWT" "$CODE" 401

TOKEN=$(hs256_jwt | tr -d '\r')
if [ -z "$TOKEN" ]; then
  echo "[FAIL] could not mint HS256 JWT (need python)"
  FAIL=$((FAIL + 1))
else
  echo "=== POST /sms/callback valid JWT ==="
  CODE=$(curl -s --max-time 20 -o /dev/null -w "%{http_code}" \
    -X POST "$BASE/sms/callback" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "$BODY" || true)
  record "callback valid JWT unknown notification" "$CODE" 404

  EVENT=$(new_uuid)
  APPT=$(new_uuid)
  PATIENT=$(new_uuid)
  echo "=== POST appointment-changed UPDATED (for callback success) ==="
  echo "eventId=$EVENT"
  CODE=$(curl -s --max-time 20 -o /dev/null -w "%{http_code}" \
    "${AUTH[@]}" \
    -X POST "$BASE/events/appointment-changed" \
    -H "Content-Type: application/json" \
    -d "{\"eventId\":\"$EVENT\",\"appointmentId\":\"$APPT\",\"patientId\":\"$PATIENT\",\"eventType\":\"UPDATED\",\"timestamp\":\"2026-10-21T17:00:00Z\",\"source\":\"runAll9Jwt\"}" \
    || true)
  record "UPDATED for JWT callback" "$CODE" 200

  sleep 1

  echo "=== GET /notifications?eventId= ==="
  LOOKUP=$(curl -s --max-time 20 -w "\nHTTPCODE:%{http_code}" \
    "${AUTH[@]}" "$BASE/notifications?eventId=$EVENT" || true)
  LOOKUP_CODE=$(printf '%s' "$LOOKUP" | tr -d '\r' | grep '^HTTPCODE:' | tail -1 | cut -d: -f2)
  LOOKUP_BODY=$(printf '%s' "$LOOKUP" | sed '$d' | tr -d '\r')
  record "GET notification by eventId" "$LOOKUP_CODE" 200
  REAL_NID=$(printf '%s' "$LOOKUP_BODY" | python -c "import json,sys; print(json.load(sys.stdin).get('notificationId',''))" 2>/dev/null | tr -d '\r')
  if [ -z "$REAL_NID" ]; then
    echo "[FAIL] could not parse notificationId from $LOOKUP_BODY"
    FAIL=$((FAIL + 1))
    echo
  else
    echo "notificationId=$REAL_NID"
    echo "=== POST /sms/callback valid JWT DELIVERED ==="
    CODE=$(curl -s --max-time 20 -o /dev/null -w "%{http_code}" \
      -X POST "$BASE/sms/callback" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TOKEN" \
      -d "$(callback_body "$REAL_NID")" || true)
    record "callback valid JWT DELIVERED" "$CODE" 200

    AFTER=$(curl -s --max-time 20 "${AUTH[@]}" "$BASE/notifications?eventId=$EVENT" | tr -d '\r' || true)
    if printf '%s' "$AFTER" | grep -q 'DELIVERED'; then
      echo "[PASS] notification status DELIVERED"
      PASS=$((PASS + 1))
    else
      echo "[FAIL] notification status not DELIVERED"
      echo "  body=$AFTER"
      FAIL=$((FAIL + 1))
    fi
    echo
  fi
fi

echo "========"
echo "PASS=$PASS  FAIL=$FAIL"
echo "========"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
