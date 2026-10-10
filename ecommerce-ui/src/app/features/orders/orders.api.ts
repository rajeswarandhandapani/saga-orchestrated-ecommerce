import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

/** Ordering context. */
export interface OrderLine {
  sku: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}
export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED';
export interface Order {
  orderId: string;
  customerId: string;
  status: OrderStatus;
  statusReason: string | null;
  cancellationRequested: boolean;
  total: number;
  currency: string;
  placedAt: string;
  lines: OrderLine[];
}

/** Orchestration (saga) - a separate service with its own view of the same order. */
export interface SagaStep {
  at: string;
  kind: 'RECEIVED' | 'SENT' | 'NOTE';
  message: string;
  detail: string | null;
  compensation: boolean;
}
export interface Saga {
  sagaId: string;
  orderId: string;
  customerId: string;
  state: string;
  total: number;
  currency: string;
  abortRequested: boolean;
  abortReason: string | null;
  failureReason: string | null;
  startedAt: string;
  updatedAt: string;
  steps: SagaStep[];
}

export const FINAL_SAGA_STATES = ['COMPLETED', 'REJECTED', 'COMPENSATED', 'CANCELLED'];

@Injectable({ providedIn: 'root' })
export class OrdersApi {
  private readonly http = inject(HttpClient);

  place(lines: { sku: string; quantity: number }[]) {
    return this.http.post<Order>('/api/orders', { lines });
  }

  list(scope: 'mine' | 'all' = 'mine') {
    return this.http.get<Order[]>('/api/orders', { params: { scope } });
  }

  get(orderId: string) {
    return this.http.get<Order>(`/api/orders/${orderId}`);
  }

  cancel(orderId: string) {
    return this.http.post<Order>(`/api/orders/${orderId}/cancel`, {});
  }

  saga(orderId: string) {
    return this.http.get<Saga>(`/api/sagas/order/${orderId}`);
  }
}
