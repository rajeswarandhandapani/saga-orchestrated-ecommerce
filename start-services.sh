#!/usr/bin/env bash
# Builds and starts all Spring services locally (infra must be up: ./start-infra.sh).
# Usage: ./start-services.sh [--skip-build]
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p .local/logs .local/pids .local/run

if [[ "${1:-}" != "--skip-build" ]]; then
  mvn -B -ntp -q package -DskipTests
fi

# module:port - discovery first, gateway last
services=(
  service-discovery:9761
  inventory-service:9002
  payment-service:9003
  order-service:9001
  saga-orchestrator-service:9004
  api-gateway:9000
)

for entry in "${services[@]}"; do
  module=${entry%%:*}; port=${entry##*:}
  pidfile=".local/pids/$module.pid"
  if [[ -f "$pidfile" ]] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
    echo "$module already running (pid $(cat "$pidfile"))"
    continue
  fi
  cp "$module/target/$module-1.0-SNAPSHOT.jar" ".local/run/$module.jar"
  nohup java -Xms64m -Xmx256m -jar ".local/run/$module.jar" > ".local/logs/$module.log" 2>&1 &
  echo $! > "$pidfile"
  printf 'Starting %-26s :%s ' "$module" "$port"
  for _ in {1..90}; do
    if curl -fsS "http://localhost:$port/actuator/health" >/dev/null 2>&1; then echo "UP"; continue 2; fi
    if ! kill -0 "$(cat "$pidfile")" 2>/dev/null; then break; fi
    sleep 2
  done
  echo "FAILED - see .local/logs/$module.log" >&2
  exit 1
done

echo
echo "Gateway : http://localhost:9000   Eureka: http://localhost:9761   Kafka UI: http://localhost:8085"
echo "Logs    : .local/logs/<service>.log"
