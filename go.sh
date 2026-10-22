#!/bin/bash
#
# Start local Kafka (Docker Compose). Postgres stays on the host.
#   ./go.sh        bring broker + topics up and wait until healthy
#   ./go.sh down   stop
#   ./go.sh down -v  stop and drop Kafka volumes
#
set -euo pipefail
cd "$(dirname "$0")"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required (Docker Desktop, or another Compose runtime)."
  exit 1
fi

if [ "${1:-}" = "down" ]; then
  shift
  docker compose down "$@"
  echo "Kafka stopped."
  exit 0
fi

echo "Starting Kafka (docker compose)..."
docker compose up -d --wait --wait-timeout 120

echo
docker compose ps
echo
echo "Topics:"
docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list

echo
echo "Kafka is listening on localhost:9092."
echo "Next:"
echo "  1. Postgres on localhost:5432 (README Database)"
echo "  2. ./mvnw spring-boot:run   (or IntelliJ DemoApplication)"
echo "  3. src/test/curl/runAll.sh"
echo
echo "If the Spring app was already running, restart it so consumers attach."
