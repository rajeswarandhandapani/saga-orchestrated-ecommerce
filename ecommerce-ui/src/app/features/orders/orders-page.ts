import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Order, OrdersApi } from './orders.api';
import { errorMessage } from '../../shared/errors';

@Component({
  selector: 'app-orders-page',
  imports: [CurrencyPipe, DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="page-head">
      <h1>{{ scope() === 'all' ? 'All orders' : 'My orders' }}</h1>
    </header>
    @if (error()) {
      <p class="alert error">{{ error() }}</p>
    }
    <table class="table">
      <thead>
        <tr><th>Order</th><th>Placed</th><th>Items</th><th class="num">Total</th><th>Status</th></tr>
      </thead>
      <tbody>
        @for (o of orders(); track o.orderId) {
          <tr>
            <td><a class="mono" [routerLink]="['/orders', o.orderId]">{{ o.orderId.slice(0, 8) }}</a></td>
            <td>{{ o.placedAt | date: 'short' }}</td>
            <td>{{ o.lines.length }}</td>
            <td class="num">{{ o.total | currency: o.currency }}</td>
            <td><span class="status status-{{ o.status }}">{{ o.status }}</span></td>
          </tr>
        } @empty {
          <tr><td colspan="5" class="muted">No orders yet.</td></tr>
        }
      </tbody>
    </table>
  `,
})
export class OrdersPage implements OnInit {
  readonly scope = input<'mine' | 'all'>('mine');
  private readonly api = inject(OrdersApi);
  readonly orders = signal<Order[]>([]);
  readonly error = signal('');

  ngOnInit(): void {
    this.api.list(this.scope()).subscribe({
      next: (o) => this.orders.set(o),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
