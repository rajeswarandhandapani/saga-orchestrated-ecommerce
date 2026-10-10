package com.rajeswaran.ecommerce.payment.domain;

import com.rajeswaran.ecommerce.platform.domain.DomainEvent;

import java.util.UUID;

public final class PaymentEvents {
    private PaymentEvents() {
    }

    public record PaymentAuthorized(UUID paymentId, UUID orderId) implements DomainEvent {
    }

    public record PaymentDeclined(UUID paymentId, UUID orderId, String reason) implements DomainEvent {
    }

    public record PaymentRefunded(UUID paymentId, UUID orderId) implements DomainEvent {
    }
}
