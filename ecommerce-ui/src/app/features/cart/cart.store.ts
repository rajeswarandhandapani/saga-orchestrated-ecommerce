import { Injectable, computed, signal } from '@angular/core';
import { Product } from '../catalog/catalog.api';

export interface CartLine {
  sku: string;
  name: string;
  unitPrice: number;
  currency: string;
  quantity: number;
}

/**
 * Client-side cart. Prices here are only for display - the Ordering context re-prices every line
 * synchronously against Inventory when the order is placed.
 */
@Injectable({ providedIn: 'root' })
export class CartStore {
  private readonly key = 'cart';
  readonly lines = signal<CartLine[]>(this.load());
  readonly count = computed(() => this.lines().reduce((n, l) => n + l.quantity, 0));
  readonly total = computed(() => this.lines().reduce((sum, l) => sum + l.unitPrice * l.quantity, 0));

  add(product: Product, quantity = 1): void {
    this.update((lines) => {
      const existing = lines.find((l) => l.sku === product.sku);
      return existing
        ? lines.map((l) => (l.sku === product.sku ? { ...l, quantity: l.quantity + quantity } : l))
        : [...lines, { sku: product.sku, name: product.name, unitPrice: product.price, currency: product.currency, quantity }];
    });
  }

  setQuantity(sku: string, quantity: number): void {
    this.update((lines) =>
      quantity <= 0 ? lines.filter((l) => l.sku !== sku) : lines.map((l) => (l.sku === sku ? { ...l, quantity } : l)),
    );
  }

  clear(): void {
    this.update(() => []);
  }

  private update(fn: (lines: CartLine[]) => CartLine[]): void {
    this.lines.update(fn);
    try {
      localStorage.setItem(this.key, JSON.stringify(this.lines()));
    } catch {
      /* storage unavailable - cart just won't survive a reload */
    }
  }

  private load(): CartLine[] {
    try {
      return JSON.parse(localStorage.getItem(this.key) ?? '[]') as CartLine[];
    } catch {
      return [];
    }
  }
}
