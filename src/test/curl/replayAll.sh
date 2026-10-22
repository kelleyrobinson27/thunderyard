#!/bin/bash

echo "=== Replaying ALL events ==="
curl -X POST http://localhost:8080/events/replay/all
echo

