import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';

/** Adds the Keycloak access token to /api calls; on 401 forgets the token so the user can sign in again. */
export const tokenInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const api = request.url.startsWith('/api/');
  const token = auth.token();
  return next(api && token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request).pipe(
    catchError((error) => {
      if (api && error.status === 401) auth.clear();
      return throwError(() => error);
    }),
  );
};
