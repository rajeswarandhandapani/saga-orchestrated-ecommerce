package com.rajeswaran.ecommerce.order.domain;

import com.rajeswaran.ecommerce.platform.domain.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount, String currency) {
    public Money {
        if (amount == null || amount.signum() < 0) {
            throw new DomainException("Money cannot be negative");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money plus(Money other) {
        if (!currency.equals(other.currency)) {
            throw new DomainException("Cannot add %s to %s".formatted(other.currency, currency));
        }
        return new Money(amount.add(other.amount), currency);
    }

    public Money times(int quantity) {
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }
}
