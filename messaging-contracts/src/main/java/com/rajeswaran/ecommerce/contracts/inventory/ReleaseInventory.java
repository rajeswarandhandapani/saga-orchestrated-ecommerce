package com.rajeswaran.ecommerce.contracts.inventory;

import java.util.UUID;

/** Compensation: give reserved stock back. Must be safe to receive when nothing is reserved. */
public record ReleaseInventory(UUID orderId, String reason) implements InventoryCommand {
}
