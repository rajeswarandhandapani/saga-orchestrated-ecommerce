package com.rajeswaran.ecommerce.contracts.order;

import java.util.UUID;

public record OrderCancelled(UUID orderId, String reason) implements OrderEvent {
}
