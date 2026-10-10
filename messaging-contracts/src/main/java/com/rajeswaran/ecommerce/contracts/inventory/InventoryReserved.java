package com.rajeswaran.ecommerce.contracts.inventory;

import java.util.UUID;

public record InventoryReserved(UUID orderId) implements InventoryEvent {
}
