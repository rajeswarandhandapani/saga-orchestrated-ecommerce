#!/usr/bin/env bash
# End-to-end check of the three saga outcomes through the gateway, using real Keycloak tokens.
# Requires: infra + services running, python3. Usage: scripts/smoke-test.sh
set -euo pipefail
GW=${GW:-http://localhost:9000}
KC=${KC:-http://localhost:8180/realms/ecommerce/protocol/openid-connect/token}

token() { # password grant is enabled on the dev client only for scripts like this one
  curl -fsS -d grant_type=password -d client_id=ecommerce-ui -d "username=$1" -d "password=$1" "$KC" |
    python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])'
}
json() { python3 -c "import sys,json;d=json.load(sys.stdin);$1"; }

ALICE=$(token alice)
ADMIN=$(token admin)

place() {
  curl -fsS -H "Authorization: Bearer $ALICE" -H 'Content-Type: application/json' -d "$1" "$GW/api/orders" |
    json 'print(d["orderId"])'
}

wait_final() { # poll until the saga is final AND the order has applied the saga's last command
  # (two contexts, two databases: the order lags the saga by one Kafka hop - eventual consistency)
  local state order
  for _ in {1..30}; do
    state=$(curl -fsS -H "Authorization: Bearer $ALICE" "$GW/api/sagas/order/$1" 2>/dev/null | json 'print(d["state"])' 2>/dev/null || echo NONE)
    order=$(curl -fsS -H "Authorization: Bearer $ALICE" "$GW/api/orders/$1" | json 'print(d["status"])')
    [[ $state == AWAITING_* || $state == NONE || $order == PENDING ]] || { echo "$state"; return; }
    sleep 1
  done
  echo "$state"
}

check() { # name, order json, expected order status, expected saga state
  local id order saga
  id=$(place "$2")
  saga=$(wait_final "$id")
  order=$(curl -fsS -H "Authorization: Bearer $ALICE" "$GW/api/orders/$id" | json 'print(d["status"])')
  if [[ $order == "$3" && $saga == "$4" ]]; then
    printf '  PASS  %-18s order=%-10s saga=%s\n' "$1" "$order" "$saga"
  else
    printf '  FAIL  %-18s order=%-10s saga=%s (expected %s/%s)\n' "$1" "$order" "$saga" "$3" "$4"; FAILED=1
  fi
  curl -fsS -H "Authorization: Bearer $ALICE" "$GW/api/sagas/order/$id" |
    json '[print("          ", s["kind"].ljust(8), s["message"], ("- "+s["detail"]) if s["detail"] else "", "[compensation]" if s["compensation"] else "") for s in d["steps"]]'
}

FAILED=0
echo "Scenarios:"
check "happy path"       '{"lines":[{"sku":"KB-001","quantity":1},{"sku":"CB-001","quantity":2}]}' CONFIRMED COMPLETED
check "out of stock"     '{"lines":[{"sku":"WC-001","quantity":50}]}'                                REJECTED  REJECTED
check "payment declined" '{"lines":[{"sku":"LT-001","quantity":1}]}'                                 REJECTED  COMPENSATED

echo "Stock (admin view):"
curl -fsS -H "Authorization: Bearer $ADMIN" "$GW/api/inventory" |
  json '[print("  ", s["sku"], "onHand", s["onHand"], "reserved", s["reserved"]) for s in d]'
exit $FAILED
