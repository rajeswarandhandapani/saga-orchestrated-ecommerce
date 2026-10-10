import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { CartStore } from './cart.store';
import { OrdersApi } from '../orders/orders.api';
import { AuthService } from '../../core/auth/auth.service';
import { errorMessage } from '../../shared/errors';

@Component({
  selector: 'app-cart-page',
  imports: [CurrencyPipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="page-head">
      <h1>Cart</h1>
      <p class="muted">Checkout calls <code>POST /api/orders</code>. The order is created as <b>PENDING</b> and the saga decides the outcome.</p>
    </header>
    @if (cart.lines().length === 0) {
      <p class="muted">Your cart is empty. <a routerLink="/">Browse the shop</a>.</p>
    } @else {
      <table class="table">
        <thead>
          <tr><th>Product</th><th>Unit price</th><th>Quantity</th><th class="num">Subtotal</th></tr>
        </thead>
        <tbody>
          @for (l of cart.lines(); track l.sku) {
            <tr>
              <td>{{ l.name }} <span class="sku">{{ l.sku }}</span></td>
              <td>{{ l.unitPrice | currency: l.currency }}</td>
              <td>
                <input class="qty" type="number" min="0" max="100" [value]="l.quantity"
                       [attr.aria-label]="'Quantity of ' + l.name"
                       (change)="cart.setQuantity(l.sku, +$any($event.target).value)" />
              </td>
              <td class="num">{{ l.unitPrice * l.quantity | currency: l.currency }}</td>
            </tr>
          }
        </tbody>
        <tfoot>
          <tr><td colspan="3">Total</td><td class="num"><strong>{{ cart.total() | currency: 'USD' }}</strong></td></tr>
        </tfoot>
      </table>
      @if (cart.total() > 1000) {
        <p class="alert warn">Totals above $1,000 are declined by the demo payment gateway, so the saga will compensate.</p>
      }
      @if (error()) {
        <p class="alert error">{{ error() }}</p>
      }
      <div class="actions">
        <button class="btn ghost" (click)="cart.clear()">Clear</button>
        <button class="btn" [disabled]="busy()" (click)="checkout()">
          {{ auth.isAuthenticated() ? (busy() ? 'Placing order…' : 'Place order') : 'Sign in to checkout' }}
        </button>
      </div>
    }
  `,
})
export class CartPage {
  readonly cart = inject(CartStore);
  readonly auth = inject(AuthService);
  private readonly orders = inject(OrdersApi);
  private readonly router = inject(Router);
  readonly busy = signal(false);
  readonly error = signal('');

  checkout(): void {
    if (!this.auth.isAuthenticated()) {
      void this.auth.login('/cart');
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.orders.place(this.cart.lines().map((l) => ({ sku: l.sku, quantity: l.quantity }))).subscribe({
      next: (order) => {
        this.cart.clear();
        void this.router.navigate(['/orders', order.orderId]);
      },
      error: (e) => {
        this.error.set(errorMessage(e));
        this.busy.set(false);
      },
    });
  }
}
