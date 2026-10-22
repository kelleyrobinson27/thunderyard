#!/bin/bash
#
# What it hits:
#   EKS-style probes: GET /actuator/health/liveness and /actuator/health/readiness,
#   plus GET /actuator/health. Expect HTTP 200 and status UP.
#
# Liveness is process-up (still 200 if Kafka is down).
# Readiness includes DB/Kafka — those should be up for this suite.

set -u
cd "$(dirname "$0")"

BASE="http://localhost:8080"
PASS=0
FAIL=0

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

check_up() {
  local name="$1"
  local body="$2"
  if printf '%s' "$body" | tr -d '\r' | grep -q '"status":"UP"'; then
    echo "[PASS] $name status UP"
    PASS=$((PASS + 1))
  else
    echo "[FAIL] $name status not UP"
    echo "  body=$body"
    FAIL=$((FAIL + 1))
  fi
  echo
}

scrape() {
  local path="$1"
  curl -s --max-time 20 -w "\nHTTPCODE:%{http_code}" "$BASE$path" || true
}

echo "ThunderYard probe suite  ($BASE)"
echo

for PATH_NAME in /actuator/health /actuator/health/liveness /actuator/health/readiness; do
  echo "=== GET $PATH_NAME ==="
  OUT=$(scrape "$PATH_NAME")
  BODY=$(printf '%s' "$OUT" | sed '$d')
  CODE=$(printf '%s' "$OUT" | tr -d '\r' | grep '^HTTPCODE:' | tail -1 | cut -d: -f2)
  record "GET $PATH_NAME" "$CODE" 200
  check_up "$PATH_NAME" "$BODY"
done

echo "========"
echo "PASS=$PASS  FAIL=$FAIL"
echo "========"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
