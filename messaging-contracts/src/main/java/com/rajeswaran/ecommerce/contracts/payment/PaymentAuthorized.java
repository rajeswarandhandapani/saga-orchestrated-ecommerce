package com.rajeswaran.ecommerce.contracts.payment;

import java.util.UUID;

public record PaymentAuthorized(UUID orderId, UUID paymentId) implements PaymentEvent {
}
