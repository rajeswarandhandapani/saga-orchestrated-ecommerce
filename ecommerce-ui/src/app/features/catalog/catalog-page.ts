import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { CatalogApi, Product } from './catalog.api';
import { CartStore } from '../cart/cart.store';
import { errorMessage } from '../../shared/errors';

@Component({
  selector: 'app-catalog-page',
  imports: [CurrencyPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="page-head">
      <h1>Shop</h1>
      <p class="muted">Catalogue and availability come from the <b>Inventory</b> context.</p>
    </header>
    @if (error()) {
      <p class="alert error">{{ error() }}</p>
    }
    <div class="grid">
      @for (p of products(); track p.sku) {
        <article class="card">
          <div class="card-top">
            <span class="sku">{{ p.sku }}</span>
            @if (p.available === 0) {
              <span class="badge bad">Out of stock</span>
            } @else if (p.available < 5) {
              <span class="badge warn">Only {{ p.available }} left</span>
            } @else {
              <span class="badge ok">{{ p.available }} in stock</span>
            }
          </div>
          <h2>{{ p.name }}</h2>
          <p class="muted small">{{ p.description }}</p>
          <div class="card-bottom">
            <strong>{{ p.price | currency: p.currency }}</strong>
            <!-- Adding unavailable items is allowed on purpose: it lets you watch the saga reject the order -->
            <button class="btn" (click)="add(p)">Add to cart</button>
          </div>
          @if (added() === p.sku) {
            <p class="small ok-text" role="status">Added to cart</p>
          }
        </article>
      } @empty {
        @if (!error()) {
          <p class="muted">Loading catalogue…</p>
        }
      }
    </div>
  `,
})
export class CatalogPage {
  private readonly api = inject(CatalogApi);
  private readonly cart = inject(CartStore);
  readonly products = signal<Product[]>([]);
  readonly error = signal('');
  readonly added = signal('');

  constructor() {
    this.api.products().subscribe({
      next: (p) => this.products.set(p),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  add(product: Product): void {
    this.cart.add(product);
    this.added.set(product.sku);
    setTimeout(() => this.added.set(''), 1500);
  }
}
