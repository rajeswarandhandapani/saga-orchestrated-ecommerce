import { HttpErrorResponse } from '@angular/common/http';

/** Extracts the RFC 9457 "detail" the services return, falling back to a generic message. */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) return 'Cannot reach the gateway - are the services running?';
    const detail = (error.error as { detail?: string } | null)?.detail;
    return detail ?? `${error.status} ${error.statusText}`;
  }
  return 'Unexpected error';
}
