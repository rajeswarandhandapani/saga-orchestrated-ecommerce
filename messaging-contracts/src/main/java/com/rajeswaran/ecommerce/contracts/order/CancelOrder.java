package com.rajeswaran.ecommerce.contracts.order;

import java.util.UUID;

public record CancelOrder(UUID orderId, String reason) implements OrderCommand {
}
