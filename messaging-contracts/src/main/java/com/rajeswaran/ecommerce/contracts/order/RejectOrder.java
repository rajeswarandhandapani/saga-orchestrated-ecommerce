package com.rajeswaran.ecommerce.contracts.order;

import java.util.UUID;

public record RejectOrder(UUID orderId, String reason) implements OrderCommand {
}
