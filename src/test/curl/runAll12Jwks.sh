#!/bin/bash
#
# What it hits:
#   Demo IdP stand-in: GET /oauth2/jwks is public RSA JWKS;
#   POST /oauth2/token needs HTTP Basic (client credentials) and returns RS256 JWT;
#   POST /sms/callback accepts that Bearer token (unknown notification → 404).
#   HMAC from runAll9Jwt still works; this suite does not need Kafka.
#
# Needs: app on :8080.

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

echo "ThunderYard JWKS / RS256 suite  ($BASE)"
echo

echo "=== GET /oauth2/jwks (public) ==="
JWKS=$(curl -s --max-time 20 -w "\nHTTPCODE:%{http_code}" "$BASE/oauth2/jwks" || true)
JWKS_CODE=$(printf '%s' "$JWKS" | tr -d '\r' | grep '^HTTPCODE:' | tail -1 | cut -d: -f2)
JWKS_BODY=$(printf '%s' "$JWKS" | sed '$d' | tr -d '\r')
record "GET JWKS" "$JWKS_CODE" 200
KTY=$(printf '%s' "$JWKS_BODY" | python -c "import json,sys; print(json.load(sys.stdin)['keys'][0]['kty'])" 2>/dev/null | tr -d '\r')
HAS_D=$(printf '%s' "$JWKS_BODY" | python -c "import json,sys; print('d' in json.load(sys.stdin)['keys'][0])" 2>/dev/null | tr -d '\r')
if [ "$KTY" = "RSA" ]; then
  check "JWKS key type is RSA" yes
else
  echo "  got kty=$KTY body=$JWKS_BODY"
  check "JWKS key type is RSA" no
fi
if [ "$HAS_D" = "False" ]; then
  check "JWKS does not expose private exponent" yes
else
  echo "  got d-present=$HAS_D"
  check "JWKS does not expose private exponent" no
fi

echo "=== POST /oauth2/token no auth ==="
CODE=$(curl -s --max-time 20 -o /dev/null -w "%{http_code}" \
  -X POST "$BASE/oauth2/token" || true)
record "token no auth" "$CODE" 401

echo "=== POST /oauth2/token HTTP Basic ==="
TOKEN_RESP=$(curl -s --max-time 20 -w "\nHTTPCODE:%{http_code}" \
  "${AUTH[@]}" -X POST "$BASE/oauth2/token" || true)
TOKEN_CODE=$(printf '%s' "$TOKEN_RESP" | tr -d '\r' | grep '^HTTPCODE:' | tail -1 | cut -d: -f2)
TOKEN_BODY=$(printf '%s' "$TOKEN_RESP" | sed '$d' | tr -d '\r')
record "token with Basic" "$TOKEN_CODE" 200
ACCESS=$(printf '%s' "$TOKEN_BODY" | python -c "import json,sys; print(json.load(sys.stdin).get('access_token',''))" 2>/dev/null | tr -d '\r')
if [ -n "$ACCESS" ]; then
  check "access_token present" yes
else
  echo "  body=$TOKEN_BODY"
  check "access_token present" no
fi

NID=$(new_uuid)
BODY=$(printf '{"notificationId":"%s","correlationId":"corr-jwks","status":"DELIVERED","providerCode":"OK"}' "$NID")

echo "=== POST /sms/callback RS256 JWT unknown id ==="
if [ -n "$ACCESS" ]; then
  CODE=$(curl -s --max-time 20 -o /dev/null -w "%{http_code}" \
    -X POST "$BASE/sms/callback" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS" \
    -d "$BODY" || true)
  record "callback RS256 unknown notification" "$CODE" 404
else
  check "callback RS256 unknown notification" no
fi

echo "========"
echo "PASS=$PASS  FAIL=$FAIL"
echo "========"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
