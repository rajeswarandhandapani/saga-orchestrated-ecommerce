#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
docker compose -f infra/compose.yml up -d --wait
echo "Postgres  : localhost:5433"
echo "Kafka     : localhost:9192   (UI: http://localhost:8085)"
echo "Keycloak  : http://localhost:8180  (admin/admin)"
