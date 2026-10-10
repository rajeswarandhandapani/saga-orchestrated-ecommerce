package com.rajeswaran.ecommerce.contracts.inventory;

import com.rajeswaran.ecommerce.contracts.LineItem;

import java.util.List;
import java.util.UUID;

/** Reserve all lines or none. */
public record ReserveInventory(UUID orderId, List<LineItem> lines) implements InventoryCommand {
}
