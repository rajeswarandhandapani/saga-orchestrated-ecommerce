// Keycloak realm + public SPA client (Authorization Code + PKCE). The API is reached through the
// dev-server proxy (/api -> gateway :9000), so the browser never talks to services directly.
export const KEYCLOAK_REALM_URL = 'http://localhost:8180/realms/ecommerce';
export const KEYCLOAK_CLIENT_ID = 'ecommerce-ui';
export const KEYCLOAK_TOKEN_ENDPOINT = `${KEYCLOAK_REALM_URL}/protocol/openid-connect/token`;
export const KEYCLOAK_LOGOUT_ENDPOINT = `${KEYCLOAK_REALM_URL}/protocol/openid-connect/logout`;
