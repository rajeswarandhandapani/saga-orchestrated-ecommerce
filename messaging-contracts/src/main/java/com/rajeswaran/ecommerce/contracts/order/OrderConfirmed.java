package com.rajeswaran.ecommerce.contracts.order;

import java.util.UUID;

/** Order reached its happy end state. */
public record OrderConfirmed(UUID orderId) implements OrderEvent {
}
