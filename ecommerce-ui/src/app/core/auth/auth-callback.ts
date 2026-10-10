import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from './auth.service';

@Component({
  selector: 'app-auth-callback',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="panel center">
      <h1>{{ error() ? 'Sign-in failed' : 'Signing you in…' }}</h1>
      @if (error()) {
        <p class="muted">{{ error() }}</p>
        <a class="btn" routerLink="/">Back to the shop</a>
      }
    </section>
  `,
})
export class AuthCallback {
  readonly error = signal('');
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  constructor() {
    const params = new URLSearchParams(location.search);
    const state = sessionStorage.getItem('oauth_state');
    const code = params.get('code');
    if (!code || !state || state !== params.get('state')) {
      this.error.set('The sign-in request expired. Please start again.');
      return;
    }
    this.auth.exchangeCodeForToken(code).subscribe({
      next: (response) => {
        this.auth.storeTokens(response);
        const returnTo = sessionStorage.getItem('return_to') || '/';
        sessionStorage.removeItem('return_to');
        void this.router.navigateByUrl(returnTo.startsWith('/auth') ? '/' : returnTo);
      },
      error: () => this.error.set('Could not complete sign-in with Keycloak.'),
    });
  }
}
