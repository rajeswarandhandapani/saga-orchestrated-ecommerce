import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

/** Inventory context's public view of a product (GET /api/products is anonymous). */
export interface Product {
  sku: string;
  name: string;
  description: string;
  price: number;
  currency: string;
  available: number;
}

@Injectable({ providedIn: 'root' })
export class CatalogApi {
  private readonly http = inject(HttpClient);

  products() {
    return this.http.get<Product[]>('/api/products');
  }
}
