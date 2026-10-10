package com.rajeswaran.ecommerce.contracts.inventory;

import com.rajeswaran.ecommerce.contracts.Command;

public sealed interface InventoryCommand extends Command permits ReserveInventory, CommitReservation, ReleaseInventory {
}
