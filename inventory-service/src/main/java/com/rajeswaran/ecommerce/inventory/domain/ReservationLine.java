package com.rajeswaran.ecommerce.inventory.domain;

import com.rajeswaran.ecommerce.platform.domain.DomainException;

public record ReservationLine(Sku sku, int quantity) {
    public ReservationLine {
        if (quantity <= 0) {
            throw new DomainException("Quantity must be positive");
        }
    }
}
