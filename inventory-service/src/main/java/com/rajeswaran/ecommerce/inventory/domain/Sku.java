package com.rajeswaran.ecommerce.inventory.domain;

import com.rajeswaran.ecommerce.platform.domain.DomainException;

/** Stock Keeping Unit - the identity of a product in the Inventory context. */
public record Sku(String value) {
    public Sku {
        if (value == null || value.isBlank()) {
            throw new DomainException("SKU must not be blank");
        }
        value = value.trim().toUpperCase();
    }

    @Override
    public String toString() {
        return value;
    }
}
