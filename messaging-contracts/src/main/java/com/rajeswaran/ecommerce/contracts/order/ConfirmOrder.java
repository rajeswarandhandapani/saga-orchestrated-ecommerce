package com.rajeswaran.ecommerce.contracts.order;

import java.util.UUID;

public record ConfirmOrder(UUID orderId) implements OrderCommand {
}
