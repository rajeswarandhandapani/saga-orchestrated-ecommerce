# Ubiquitous language

Terms are defined **per context**. If a term isn't listed for a context, that context doesn't use it.

## Ordering
- **Order**: a customer's request to buy some lines. The aggregate root.
- **Order line**: SKU, product name, quantity and **unit price snapshot**. It's immutable once the order is placed.
- **Place order**: re-prices every line against Inventory (sync), then creates a PENDING order and emits `OrderPlaced`.
- **PENDING / CONFIRMED / REJECTED / CANCELLED**: the order lifecycle. Only PENDING can change.
- **Request cancellation**: the customer's *intent* to cancel. The order stays PENDING until the saga finishes unwinding.

## Inventory
- **SKU**: the identity of a sellable item.
- **On hand**: physical units in the warehouse.
- **Reserved**: units promised to orders that are still in flight.
- **Available** = on hand − reserved.
- **Reservation**: units held for one order, **all lines or none**. RESERVED → COMMITTED (shipped) or RELEASED (given back).
- **Restock**: an admin adds units on hand.

## Payments
- **Payment**: one attempt to charge an order's total. AUTHORIZED, DECLINED, or REFUNDED.
- **Payment gateway**: the external provider (faked here; it declines anything over 1000.00).
- **Refund**: the compensation for an authorized payment.

## Orchestration
- **Saga**: a long-running business process spread over several contexts, with no distributed transaction.
- **Command**: "please do X", sent to exactly one context (`ReserveInventory`, `AuthorizePayment`, `ConfirmOrder` …).
- **Event**: "X happened", published by the context that owns X (`InventoryReserved`, `PaymentDeclined` …).
- **Compensation**: a semantic undo of a step that already committed (`ReleaseInventory`, `RefundPayment`).
- **Abort**: a flag that says "unwind at the next reply". It's set by a customer cancel or a step timeout.
- **COMPLETED / REJECTED / COMPENSATED / CANCELLED**: the saga's final states. See `saga-flow.md`.

## Platform (technical, not domain)
- **Outbox**: the table where messages are written in the same transaction as state changes. A relay publishes them to Kafka.
- **Inbox**: the table of processed message ids, so a redelivered message is handled once.
- **Correlation id**: one id that follows an HTTP request through every Kafka hop (`X-Correlation-Id`, shown in log lines).
