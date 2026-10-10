package com.rajeswaran.ecommerce.contracts.inventory;

import java.util.UUID;

public record ReservationCommitted(UUID orderId) implements InventoryEvent {
}
