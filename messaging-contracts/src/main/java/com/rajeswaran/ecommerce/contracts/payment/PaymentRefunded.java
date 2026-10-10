package com.rajeswaran.ecommerce.contracts.payment;

import java.util.UUID;

public record PaymentRefunded(UUID orderId) implements PaymentEvent {
}
