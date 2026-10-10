package com.rajeswaran.ecommerce.order.domain;

import com.rajeswaran.ecommerce.platform.domain.DomainEvent;

public final class OrderEvents {
    private OrderEvents() {
    }

    public record OrderPlaced(Order order) implements DomainEvent {
    }

    public record CancellationRequested(OrderId orderId) implements DomainEvent {
    }

    public record OrderConfirmed(OrderId orderId) implements DomainEvent {
    }

    public record OrderRejected(OrderId orderId, String reason) implements DomainEvent {
    }

    public record OrderCancelled(OrderId orderId, String reason) implements DomainEvent {
    }
}
