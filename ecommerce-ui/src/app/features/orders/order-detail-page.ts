import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription, catchError, forkJoin, of, switchMap, takeWhile, timer } from 'rxjs';
import { FINAL_SAGA_STATES, Order, OrdersApi, Saga } from './orders.api';
import { SagaTimeline } from './saga-timeline';
import { errorMessage } from '../../shared/errors';

/**
 * The order (Ordering context) and its saga (Orchestration) are fetched from two different services.
 * Because the outcome is decided asynchronously, the page polls until both reach a final state.
 */
@Component({
  selector: 'app-order-detail-page',
  imports: [CurrencyPipe, DatePipe, RouterLink, SagaTimeline],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <p><a routerLink="/orders">← My orders</a></p>
    @if (error()) {
      <p class="alert error">{{ error() }}</p>
    }
    @if (order(); as o) {
      <header class="page-head row">
        <div>
          <h1>Order <span class="mono">{{ o.orderId.slice(0, 8) }}</span></h1>
          <p class="muted">Placed {{ o.placedAt | date: 'medium' }}</p>
        </div>
        <span class="status status-{{ o.status }}">{{ o.status }}</span>
      </header>
      @if (o.statusReason) {
        <p class="alert" [class.error]="o.status === 'REJECTED'">{{ o.statusReason }}</p>
      }
      <div class="two-col">
        <section class="panel">
          <h2>Ordering context</h2>
          <table class="table compact">
            <tbody>
              @for (l of o.lines; track l.sku) {
                <tr>
                  <td>{{ l.quantity }} × {{ l.productName }}</td>
                  <td class="num">{{ l.lineTotal | currency: o.currency }}</td>
                </tr>
              }
            </tbody>
            <tfoot>
              <tr><td>Total</td><td class="num"><strong>{{ o.total | currency: o.currency }}</strong></td></tr>
            </tfoot>
          </table>
          @if (o.status === 'PENDING') {
            <button class="btn ghost" [disabled]="o.cancellationRequested || cancelling()" (click)="cancel(o)">
              {{ o.cancellationRequested ? 'Cancellation requested' : 'Cancel order' }}
            </button>
          }
          @if (polling()) {
            <p class="muted small" role="status">Waiting for the saga to finish…</p>
          }
        </section>
        <section class="panel">
          <h2>Saga orchestrator</h2>
          @if (saga(); as s) {
            <app-saga-timeline [saga]="s" />
          } @else {
            <p class="muted">The orchestrator has not picked up OrderPlaced yet…</p>
          }
        </section>
      </div>
    }
  `,
})
export class OrderDetailPage implements OnInit {
  readonly id = input.required<string>();
  private readonly api = inject(OrdersApi);
  private pollSub?: Subscription;
  readonly order = signal<Order | null>(null);
  readonly saga = signal<Saga | null>(null);
  readonly error = signal('');
  readonly cancelling = signal(false);
  readonly done = computed(
    () => this.order()?.status !== 'PENDING' && FINAL_SAGA_STATES.includes(this.saga()?.state ?? ''),
  );
  readonly polling = signal(true);

  constructor() {
    inject(DestroyRef).onDestroy(() => this.pollSub?.unsubscribe());
  }

  ngOnInit(): void {
    this.poll();
  }

  cancel(order: Order): void {
    this.cancelling.set(true);
    this.api.cancel(order.orderId).subscribe({
      next: (o) => {
        this.order.set(o);
        this.cancelling.set(false);
        this.poll();
      },
      error: (e) => {
        this.error.set(errorMessage(e));
        this.cancelling.set(false);
      },
    });
  }

  private poll(): void {
    this.pollSub?.unsubscribe();
    this.polling.set(true);
    this.pollSub = timer(0, 1000)
      .pipe(
        switchMap(() =>
          forkJoin({
            order: this.api.get(this.id()),
            saga: this.api.saga(this.id()).pipe(catchError(() => of(null))), // 404 until the saga starts
          }),
        ),
        takeWhile(() => !this.done(), true),
      )
      .subscribe({
        next: ({ order, saga }) => {
          this.order.set(order);
          this.saga.set(saga);
        },
        error: (e) => {
          this.error.set(errorMessage(e));
          this.polling.set(false);
        },
        complete: () => this.polling.set(false),
      });
  }
}
