package com.rajeswaran.ecommerce.inventory.domain;

import com.rajeswaran.ecommerce.platform.domain.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** List price of a product. Inventory is the system of record for prices in this demo. */
public record Price(BigDecimal amount, String currency) {
    public Price {
        if (amount == null || amount.signum() < 0) {
            throw new DomainException("Price must be zero or positive");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }
}
