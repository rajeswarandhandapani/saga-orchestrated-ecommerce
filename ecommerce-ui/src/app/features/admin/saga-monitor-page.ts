import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { switchMap, timer } from 'rxjs';
import { AdminApi } from './admin.api';
import { Saga } from '../orders/orders.api';
import { errorMessage } from '../../shared/errors';

@Component({
  selector: 'app-saga-monitor-page',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="page-head">
      <h1>Saga monitor</h1>
      <p class="muted">Latest 100 PlaceOrder sagas, refreshed every 3 seconds.</p>
    </header>
    @if (error()) {
      <p class="alert error">{{ error() }}</p>
    }
    <table class="table">
      <thead>
        <tr><th>Order</th><th>Started</th><th class="num">Total</th><th>State</th><th>Reason</th></tr>
      </thead>
      <tbody>
        @for (s of sagas(); track s.sagaId) {
          <tr>
            <td><a class="mono" [routerLink]="['/orders', s.orderId]">{{ s.orderId.slice(0, 8) }}</a></td>
            <td>{{ s.startedAt | date: 'medium' }}</td>
            <td class="num">{{ s.total | currency: s.currency }}</td>
            <td><span class="state state-{{ s.state }}">{{ s.state }}</span></td>
            <td class="muted small">{{ s.failureReason || s.abortReason || '' }}</td>
          </tr>
        } @empty {
          <tr><td colspan="5" class="muted">No sagas yet.</td></tr>
        }
      </tbody>
    </table>
  `,
})
export class SagaMonitorPage {
  private readonly api = inject(AdminApi);
  readonly sagas = signal<Saga[]>([]);
  readonly error = signal('');

  constructor() {
    const sub = timer(0, 3000)
      .pipe(switchMap(() => this.api.sagas()))
      .subscribe({ next: (s) => this.sagas.set(s), error: (e) => this.error.set(errorMessage(e)) });
    inject(DestroyRef).onDestroy(() => sub.unsubscribe());
  }
}
