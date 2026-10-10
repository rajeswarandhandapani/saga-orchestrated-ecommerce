package com.rajeswaran.ecommerce.order.domain;

import com.rajeswaran.ecommerce.platform.domain.AggregateRoot;
import com.rajeswaran.ecommerce.platform.domain.DomainException;

import java.time.Instant;
import java.util.List;

/**
 * Aggregate root of the Ordering context.
 * <pre>
 * PENDING ─confirm→ CONFIRMED
 * PENDING ─reject──→ REJECTED     (out of stock, payment declined ...)
 * PENDING ─cancel──→ CANCELLED    (customer asked, saga unwound)
 * </pre>
 * The Order does not decide <i>when</i> to confirm - that is the saga's job. It only guards which
 * transitions are legal. Repeating a transition to the same final state is a no-op (idempotency).
 */
public class Order extends AggregateRoot<OrderId> {

    private final OrderId id;
    private final CustomerId customerId;
    private final List<OrderLine> lines;
    private final Instant placedAt;
    private OrderStatus status;
    private boolean cancellationRequested;
    private String statusReason;

    public Order(OrderId id, CustomerId customerId, List<OrderLine> lines, Instant placedAt, OrderStatus status,
                 boolean cancellationRequested, String statusReason) {
        this.id = id;
        this.customerId = customerId;
        this.lines = List.copyOf(lines);
        this.placedAt = placedAt;
        this.status = status;
        this.cancellationRequested = cancellationRequested;
        this.statusReason = statusReason;
    }

    public static Order place(CustomerId customerId, List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new DomainException("An order needs at least one line");
        }
        if (lines.stream().map(OrderLine::sku).distinct().count() != lines.size()) {
            throw new DomainException("Each SKU may appear only once per order");
        }
        String currency = lines.getFirst().unitPrice().currency();
        if (lines.stream().anyMatch(l -> !l.unitPrice().currency().equals(currency))) {
            throw new DomainException("All lines must use the same currency");
        }
        var order = new Order(OrderId.newId(), customerId, lines, Instant.now(), OrderStatus.PENDING, false, null);
        order.raise(new OrderEvents.OrderPlaced(order));
        return order;
    }

    public Money total() {
        return lines.stream().map(OrderLine::lineTotal)
                .reduce(Money.zero(lines.getFirst().unitPrice().currency()), Money::plus);
    }

    /** Customer intent only; the saga decides whether and how to unwind. */
    public void requestCancellation(CustomerId requester) {
        if (!customerId.equals(requester)) {
            throw new DomainException("Only the buyer can cancel this order");
        }
        if (status != OrderStatus.PENDING) {
            throw new DomainException("Order is already " + status + " and can no longer be cancelled");
        }
        if (cancellationRequested) {
            return;
        }
        cancellationRequested = true;
        raise(new OrderEvents.CancellationRequested(id));
    }

    public void confirm() {
        if (transitionTo(OrderStatus.CONFIRMED, null)) {
            raise(new OrderEvents.OrderConfirmed(id));
        }
    }

    public void reject(String reason) {
        if (transitionTo(OrderStatus.REJECTED, reason)) {
            raise(new OrderEvents.OrderRejected(id, reason));
        }
    }

    public void cancel(String reason) {
        if (transitionTo(OrderStatus.CANCELLED, reason)) {
            raise(new OrderEvents.OrderCancelled(id, reason));
        }
    }

    private boolean transitionTo(OrderStatus target, String reason) {
        if (status == target) {
            return false; // duplicate command - already there
        }
        if (status.isFinal()) {
            throw new DomainException("Order %s is %s; cannot become %s".formatted(id, status, target));
        }
        status = target;
        statusReason = reason;
        return true;
    }

    @Override
    public OrderId id() {
        return id;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public List<OrderLine> lines() {
        return lines;
    }

    public Instant placedAt() {
        return placedAt;
    }

    public OrderStatus status() {
        return status;
    }

    public boolean cancellationRequested() {
        return cancellationRequested;
    }

    public String statusReason() {
        return statusReason;
    }
}
