import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Saga } from '../orders/orders.api';

export interface StockItem {
  sku: string;
  name: string;
  onHand: number;
  reserved: number;
  available: number;
}

@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);

  stock() {
    return this.http.get<StockItem[]>('/api/inventory');
  }

  restock(sku: string, quantity: number) {
    return this.http.post<StockItem>(`/api/inventory/${sku}/restock`, { quantity });
  }

  sagas() {
    return this.http.get<Saga[]>('/api/sagas');
  }
}
