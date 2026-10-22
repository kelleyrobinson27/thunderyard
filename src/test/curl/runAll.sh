#!/bin/bash
#
# What it hits:
#   Numbered suites (runAll1Http … runAll12Jwks). One PASS/FAIL line each.
#   Alias wrappers runAllEvents.sh / runAllAppointmentEvents.sh are not run here.
#
set -u
cd "$(dirname "$0")"

FAIL=0

while IFS= read -r script; do
  [ -z "$script" ] && continue
  name="${script%.sh}"
  if bash "./$script" >/dev/null 2>&1; then
    printf '%s  PASS\n' "$name"
  else
    printf '%s  FAIL\n' "$name"
    FAIL=$((FAIL + 1))
  fi
done < <(printf '%s\n' runAll[0-9]*.sh | sort -V)

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
exit 0
