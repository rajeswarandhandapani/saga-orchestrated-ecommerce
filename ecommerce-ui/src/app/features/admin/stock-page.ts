import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AdminApi, StockItem } from './admin.api';
import { errorMessage } from '../../shared/errors';

@Component({
  selector: 'app-stock-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="page-head">
      <h1>Stock</h1>
      <p class="muted"><b>Reserved</b> units are held by in-flight sagas; they become a stock deduction on CommitReservation or return on ReleaseInventory.</p>
    </header>
    @if (error()) {
      <p class="alert error">{{ error() }}</p>
    }
    <table class="table">
      <thead>
        <tr><th>SKU</th><th>Product</th><th class="num">On hand</th><th class="num">Reserved</th><th class="num">Available</th><th>Restock</th></tr>
      </thead>
      <tbody>
        @for (s of stock(); track s.sku) {
          <tr>
            <td class="mono">{{ s.sku }}</td>
            <td>{{ s.name }}</td>
            <td class="num">{{ s.onHand }}</td>
            <td class="num">{{ s.reserved }}</td>
            <td class="num">{{ s.available }}</td>
            <td>
              <button class="btn small" (click)="restock(s, 5)">+5</button>
              <button class="btn small" (click)="restock(s, 20)">+20</button>
            </td>
          </tr>
        }
      </tbody>
    </table>
    <button class="btn ghost" (click)="load()">Refresh</button>
  `,
})
export class StockPage {
  private readonly api = inject(AdminApi);
  readonly stock = signal<StockItem[]>([]);
  readonly error = signal('');

  constructor() {
    this.load();
  }

  load(): void {
    this.api.stock().subscribe({ next: (s) => this.stock.set(s), error: (e) => this.error.set(errorMessage(e)) });
  }

  restock(item: StockItem, quantity: number): void {
    this.api.restock(item.sku, quantity).subscribe({
      next: (updated) => this.stock.update((all) => all.map((s) => (s.sku === updated.sku ? updated : s))),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
