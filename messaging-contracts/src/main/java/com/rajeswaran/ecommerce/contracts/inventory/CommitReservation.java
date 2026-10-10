package com.rajeswaran.ecommerce.contracts.inventory;

import java.util.UUID;

/** Turn a reservation into a real stock deduction (order confirmed). */
public record CommitReservation(UUID orderId) implements InventoryCommand {
}
