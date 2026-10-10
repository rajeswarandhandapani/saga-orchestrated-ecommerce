package com.rajeswaran.ecommerce.contracts.inventory;

import java.util.UUID;

public record InventoryReleased(UUID orderId) implements InventoryEvent {
}
