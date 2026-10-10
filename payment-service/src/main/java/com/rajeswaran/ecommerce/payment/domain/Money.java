package com.rajeswaran.ecommerce.payment.domain;

import com.rajeswaran.ecommerce.platform.domain.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Each context owns its own Money value object; there is no shared "common Money" class. */
public record Money(BigDecimal amount, String currency) {
    public Money {
        if (amount == null || amount.signum() <= 0) {
            throw new DomainException("Payment amount must be positive");
        }
        if (currency == null || currency.length() != 3) {
            throw new DomainException("Currency must be an ISO-4217 code");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isGreaterThan(BigDecimal other) {
        return amount.compareTo(other) > 0;
    }
}
