# ecommerce-ui

Angular 22 SPA for the saga-orchestrated e-commerce demo. See the [root README](../README.md).

- `npm start`: dev server on http://localhost:4300. `/api` is proxied to the gateway on :9000 (`proxy.conf.json`).
- `npm test`: Vitest unit tests.
- Auth: OIDC Authorization Code + PKCE against Keycloak realm `ecommerce`, client `ecommerce-ui` (`src/app/core/auth`).
- Features are grouped by the bounded context they talk to: `features/catalog` (Inventory), `features/orders` (Ordering + Orchestrator), `features/cart` (client-only), `features/admin`.
