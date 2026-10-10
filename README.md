# Saga-Orchestrated E-Commerce

A learning project for **Domain-Driven Design**, **bounded contexts** and the **orchestrated saga** pattern.
Three contexts (Ordering, Inventory, Payments) each own their model and database. A saga orchestrator coordinates the "place order" process over Kafka, compensating when something fails.

| | Version |
|---|---|
| Java | 25 |
| Spring Boot / Spring Cloud | 4.1.1 / 2025.1.3 (Gateway, Eureka, LoadBalancer) |
| Kafka (KRaft) | `apache/kafka:4.3.1` + `kafbat/kafka-ui:v1.5.0` |
| Keycloak | `quay.io/keycloak/keycloak:26.8.0` |
| PostgreSQL | `postgres:18.6` (one database per context) |
| Angular | 22.2 (standalone, signals, zoneless, Vitest) |

## Read these first
1. [docs/context-map.md](docs/context-map.md): the contexts, how they relate, and how "product", "order" and "money" mean different things in each.
2. [docs/saga-flow.md](docs/saga-flow.md): the state machine, message sequence, and why the outbox and inbox exist.
3. [docs/ubiquitous-language.md](docs/ubiquitous-language.md): the glossary for each context.

## Architecture

```
Angular :4300 ──/api proxy──► API Gateway :9000 (JWT check, routing via Eureka :9761)
                                 │
     ┌───────────────────────────┼──────────────────────────┬──────────────────────────┐
order-service :9001      inventory-service :9002     payment-service :9003    saga-orchestrator :9004
  Ordering context  ─sync─►  Inventory context           Payments context         PlaceOrderSaga
   db: orders                 db: inventory               db: payments             db: sagas
     └────────── outbox ──► Kafka topics  <context>.commands / <context>.events ◄── outbox ─┘
```

| Interaction | Style | Why |
|---|---|---|
| UI → services | sync REST through the gateway | the user is waiting |
| Ordering → Inventory price lookup | **sync** REST (`@LoadBalanced RestClient` + HTTP interface, behind an ACL) | an order can't be built without current prices |
| Reserve stock, take payment, confirm/reject | **async** Kafka commands and events | spans 3 contexts; needs retries, compensation, and no distributed transaction |

### Module layout
Every domain service uses the same hexagonal layout:
```
domain/          aggregates, value objects, domain events, repository ports   (plain Java)
application/     use cases; translates domain events → integration events (outbox)
infrastructure/  JPA adapters + mappers, REST clients (ACL), gateways
interfaces/      REST controllers, Kafka listeners
```
| Module | What it is |
|---|---|
| `messaging-contracts` | Published Language: command/event records + topic names. **The only shared model.** |
| `platform-lib` | Technical building blocks only: outbox, inbox, correlation id, Keycloak role mapping, error mapping. Its test-jar holds the Testcontainers harness. |
| `service-discovery` | Eureka server |
| `api-gateway` | Spring Cloud Gateway (WebFlux), OAuth2 resource server, CORS, blocks `/internal/**` |
| `order-service`, `inventory-service`, `payment-service` | The three bounded contexts |
| `saga-orchestrator-service` | The PlaceOrder process manager |
| `ecommerce-ui` | Angular SPA with hand-rolled OIDC Auth Code + PKCE against Keycloak |

## Run it

Prerequisites: Docker, Java 25, Maven 3.9+, Node 24.

```bash
./start-infra.sh                 # Postgres, Kafka, Kafka UI, Keycloak (realm auto-imported)
./start-services.sh              # builds and starts the 6 Spring apps, waiting for each to be healthy
scripts/smoke-test.sh            # optional: runs the 3 saga scenarios via curl
cd ecommerce-ui && npm install && npm start    # http://localhost:4300
```

Stop with `./stop-services.sh` and `docker compose -f infra/compose.yml down` (add `-v` to wipe data).

| URL | |
|---|---|
| http://localhost:4300 | Shop UI |
| http://localhost:9000 | API gateway |
| http://localhost:9761 | Eureka dashboard |
| http://localhost:8085 | Kafka UI: watch `*.commands` / `*.events` |
| http://localhost:8180 | Keycloak admin (admin/admin) |

**Users** (realm `ecommerce`): `alice/alice` and `bob/bob` are customers; `admin/admin` has ADMIN + CUSTOMER.
Service logs are in `.local/logs/`. Every line carries the correlation id, so you can grep one order across all services.

## Try these scenarios in the UI

| Do this | Expected saga | Expected order |
|---|---|---|
| Buy a keyboard and a cable | `COMPLETED` (reserve → pay → commit + confirm) | CONFIRMED, stock goes down |
| Buy 5 × HD Webcam (only 3 in stock), or the Laptop Dock (0) | `REJECTED`; no payment attempted | REJECTED: "Insufficient stock…" |
| Buy the Ultrabook Laptop ($1,299) | `COMPENSATED`: payment declined → **ReleaseInventory** | REJECTED: "Card limit exceeded…", stock restored |
| Place any order and click **Cancel order** immediately | `CANCELLED` (release, plus a refund if already paid) | CANCELLED |

The order page shows the order (from order-service) next to the saga timeline (from the orchestrator), and red rows are compensations.
As admin, use **Sagas** to watch every saga, and **Stock** to see *reserved* units move and to restock.

Things to try while learning:
- Stop `payment-service` (`kill $(cat .local/pids/payment-service.pid)`) and place an order. The saga waits in AWAITING_PAYMENT, and after 60s it is flagged to abort. Start the service again (`./start-services.sh --skip-build`): the queued command is processed, and the saga compensates and ends CANCELLED.
  (After a restart, the gateway may return 503 for that service for up to ~30s until its Eureka cache refreshes.)
- Stop `inventory-service` and try to place an order. You get a 503, because the *synchronous* dependency is the price lookup. This is the trade-off of sync calls.
- Look at the `outbox_message` and `processed_message` tables in any database (`psql -h localhost -p 5433 -U orders_user orders`, password `orders_pw`).

## Tests

```bash
mvn verify                       # domain + saga unit tests, plus Testcontainers integration tests (needs Docker)
cd ecommerce-ui && npm test      # Vitest
```
- **Unit (no Spring):** `OrderTest`, `ReservationTest`, `PaymentTest`, `PlaceOrderSagaTest`.
- **Integration (real Postgres + Kafka):** `*MessagingIntegrationTest` in each service. They prove outbox publication, that a redelivered message is processed once, and the orchestrator's command routing.

## API summary (through the gateway)

| Method & path | Context | Who |
|---|---|---|
| `GET /api/products` | Inventory | anyone |
| `POST /api/orders` `{lines:[{sku,quantity}]}` → 202 | Ordering | customer |
| `GET /api/orders[?scope=all]`, `GET /api/orders/{id}`, `POST /api/orders/{id}/cancel` | Ordering | owner / admin |
| `GET /api/sagas/order/{orderId}` | Orchestrator | owner / admin |
| `GET /api/sagas` | Orchestrator | admin |
| `GET /api/inventory`, `POST /api/inventory/{sku}/restock` `{quantity}` | Inventory | admin |
| `GET /api/payments`, `GET /api/payments/order/{orderId}` | Payments | owner / admin |
| `GET /internal/products?skus=` | Inventory | service-to-service only (blocked at the gateway) |

> The Keycloak client has the *direct access (password) grant* enabled only so that `scripts/smoke-test.sh` can get tokens. The SPA uses Authorization Code + PKCE. Turn the password grant off for anything beyond local learning.
