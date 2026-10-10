#!/usr/bin/env bash
# Stops only the JVMs started by start-services.sh. Infra: docker compose -f infra/compose.yml down [-v]
set -euo pipefail
cd "$(dirname "$0")"
for pidfile in .local/pids/*.pid; do
  [[ -e "$pidfile" ]] || continue
  pid=$(cat "$pidfile")
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" && echo "Stopped $(basename "$pidfile" .pid)"
  fi
  rm -f "$pidfile"
done
