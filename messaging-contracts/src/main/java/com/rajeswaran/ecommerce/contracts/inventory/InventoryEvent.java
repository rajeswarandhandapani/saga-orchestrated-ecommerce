package com.rajeswaran.ecommerce.contracts.inventory;

import com.rajeswaran.ecommerce.contracts.Event;

public sealed interface InventoryEvent extends Event
        permits InventoryReserved, InventoryRejected, ReservationCommitted, InventoryReleased {
}
