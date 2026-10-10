package com.rajeswaran.ecommerce.contracts.order;

import com.rajeswaran.ecommerce.contracts.Event;

public sealed interface OrderEvent extends Event
        permits OrderPlaced, OrderCancellationRequested, OrderConfirmed, OrderRejected, OrderCancelled {
}
