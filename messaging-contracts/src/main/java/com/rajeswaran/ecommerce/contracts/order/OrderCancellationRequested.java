package com.rajeswaran.ecommerce.contracts.order;

import java.util.UUID;

/** Customer asked to cancel a PENDING order; the orchestrator decides how to unwind. */
public record OrderCancellationRequested(UUID orderId) implements OrderEvent {
}
