package com.rajeswaran.ecommerce.inventory.domain;

import com.rajeswaran.ecommerce.platform.domain.DomainEvent;

import java.util.UUID;

/** Domain events of the Inventory context (internal; translated to contracts by the application layer). */
public final class ReservationEvents {
    private ReservationEvents() {
    }

    public record StockReserved(UUID orderId) implements DomainEvent {
    }

    public record ReservationCommitted(UUID orderId) implements DomainEvent {
    }

    public record ReservationReleased(UUID orderId) implements DomainEvent {
    }
}
