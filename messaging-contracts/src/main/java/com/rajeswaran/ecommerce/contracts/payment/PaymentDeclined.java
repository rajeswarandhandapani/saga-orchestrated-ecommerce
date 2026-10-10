package com.rajeswaran.ecommerce.contracts.payment;

import java.util.UUID;

public record PaymentDeclined(UUID orderId, String reason) implements PaymentEvent {
}
