package com.rajeswaran.ecommerce.payment.infrastructure.gateway;

import com.rajeswaran.ecommerce.payment.domain.Money;
import com.rajeswaran.ecommerce.payment.domain.PaymentGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/** Stand-in for a real PSP: declines anything above a configurable limit so compensation can be demoed. */
@Component
class FakePaymentGateway implements PaymentGateway {

    private final BigDecimal declineAbove;

    FakePaymentGateway(@Value("${payments.gateway.decline-above:1000.00}") BigDecimal declineAbove) {
        this.declineAbove = declineAbove;
    }

    @Override
    public Result authorize(String customerId, Money amount) {
        if (amount.isGreaterThan(declineAbove)) {
            return new Declined("Card limit exceeded: %s %s is above %s".formatted(amount.amount(), amount.currency(), declineAbove));
        }
        return new Approved(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
