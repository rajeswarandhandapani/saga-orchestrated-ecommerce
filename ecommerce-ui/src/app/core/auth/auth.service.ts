import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import {
  KEYCLOAK_CLIENT_ID,
  KEYCLOAK_LOGOUT_ENDPOINT,
  KEYCLOAK_REALM_URL,
  KEYCLOAK_TOKEN_ENDPOINT,
} from '../config';

interface TokenResponse {
  access_token: string;
  id_token?: string;
}
interface Claims {
  exp?: number;
  sub?: string;
  preferred_username?: string;
  name?: string;
  realm_access?: { roles: string[] };
}

/** Hand-rolled OIDC Authorization Code + PKCE flow against Keycloak (same approach as baas-bank-ui). */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  readonly token = signal(sessionStorage.getItem('access_token'));
  readonly claims = computed<Claims>(() => {
    try {
      const part = this.token()?.split('.')[1] ?? '';
      return JSON.parse(atob(part.replace(/-/g, '+').replace(/_/g, '/'))) as Claims;
    } catch {
      return {};
    }
  });
  readonly roles = computed(() => this.claims().realm_access?.roles ?? []);
  readonly isAdmin = computed(() => this.roles().includes('ADMIN'));
  readonly name = computed(() => this.claims().name || this.claims().preferred_username || '');

  isAuthenticated(): boolean {
    return !!this.token() && (this.claims().exp ?? 0) > Date.now() / 1000;
  }

  async login(returnTo = location.pathname): Promise<void> {
    const verifier = this.random();
    const state = this.random();
    sessionStorage.setItem('pkce_verifier', verifier);
    sessionStorage.setItem('oauth_state', state);
    sessionStorage.setItem('return_to', returnTo);
    const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier));
    const query = new URLSearchParams({
      client_id: KEYCLOAK_CLIENT_ID,
      response_type: 'code',
      scope: 'openid profile email',
      redirect_uri: location.origin + '/auth/callback',
      state,
      code_challenge: this.base64(new Uint8Array(digest)),
      code_challenge_method: 'S256',
    });
    location.assign(`${KEYCLOAK_REALM_URL}/protocol/openid-connect/auth?${query}`);
  }

  exchangeCodeForToken(code: string) {
    const body = new HttpParams()
      .set('grant_type', 'authorization_code')
      .set('code', code)
      .set('client_id', KEYCLOAK_CLIENT_ID)
      .set('redirect_uri', location.origin + '/auth/callback')
      .set('code_verifier', sessionStorage.getItem('pkce_verifier') ?? '');
    return this.http.post<TokenResponse>(KEYCLOAK_TOKEN_ENDPOINT, body);
  }

  storeTokens(response: TokenResponse): void {
    sessionStorage.setItem('access_token', response.access_token);
    if (response.id_token) sessionStorage.setItem('id_token', response.id_token);
    this.token.set(response.access_token);
    sessionStorage.removeItem('pkce_verifier');
    sessionStorage.removeItem('oauth_state');
  }

  clear(): void {
    sessionStorage.removeItem('access_token');
    this.token.set(null);
  }

  logout(): void {
    const idToken = sessionStorage.getItem('id_token');
    this.clear();
    sessionStorage.removeItem('id_token');
    const query = new URLSearchParams({ client_id: KEYCLOAK_CLIENT_ID, post_logout_redirect_uri: location.origin });
    if (idToken) query.set('id_token_hint', idToken);
    location.assign(`${KEYCLOAK_LOGOUT_ENDPOINT}?${query}`);
  }

  private random(): string {
    return this.base64(crypto.getRandomValues(new Uint8Array(32)));
  }

  private base64(bytes: Uint8Array): string {
    return btoa(String.fromCharCode(...bytes)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  }
}
