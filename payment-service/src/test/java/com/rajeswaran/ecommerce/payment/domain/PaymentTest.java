package com.rajeswaran.ecommerce.payment.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTest {

    private final PaymentGateway approveAll = (customer, amount) -> new PaymentGateway.Approved("OK");
    private final PaymentGateway declineAll = (customer, amount) -> new PaymentGateway.Declined("no funds");

    @Test
    void approvedPaymentIsAuthorized() {
        var payment = Payment.authorize(UUID.randomUUID(), "alice", new Money(new BigDecimal("10"), "USD"), approveAll);
        assertThat(payment.status()).isEqualTo(PaymentStatus.AUTHORIZED);
        assertThat(payment.pullDomainEvents()).singleElement().isInstanceOf(PaymentEvents.PaymentAuthorized.class);
    }

    @Test
    void declinedPaymentRaisesDeclined() {
        var payment = Payment.authorize(UUID.randomUUID(), "alice", new Money(new BigDecimal("10"), "USD"), declineAll);
        assertThat(payment.status()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(payment.pullDomainEvents()).singleElement().isInstanceOf(PaymentEvents.PaymentDeclined.class);
    }

    @Test
    void refundIsIdempotent() {
        var payment = Payment.authorize(UUID.randomUUID(), "alice", new Money(new BigDecimal("10"), "USD"), approveAll);
        payment.pullDomainEvents();
        payment.refund("cancelled");
        payment.refund("cancelled");
        assertThat(payment.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.pullDomainEvents()).hasSize(2).allMatch(e -> e instanceof PaymentEvents.PaymentRefunded);
    }
}
