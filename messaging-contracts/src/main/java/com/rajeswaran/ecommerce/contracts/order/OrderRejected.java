package com.rajeswaran.ecommerce.contracts.order;

import java.util.UUID;

public record OrderRejected(UUID orderId, String reason) implements OrderEvent {
}
