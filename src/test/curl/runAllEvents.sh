#!/bin/bash
#
# What it hits:
#   Wrapper — same as runAll1Http.sh (every event POST, validation, list, replay).
#
cd "$(dirname "$0")"
exec ./runAll1Http.sh
