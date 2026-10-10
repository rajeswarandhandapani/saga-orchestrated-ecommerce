package com.rajeswaran.ecommerce.contracts.inventory;

import java.util.UUID;

public record InventoryRejected(UUID orderId, String reason) implements InventoryEvent {
}
