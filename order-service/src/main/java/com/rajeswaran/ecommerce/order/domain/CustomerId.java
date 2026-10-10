package com.rajeswaran.ecommerce.order.domain;

import com.rajeswaran.ecommerce.platform.domain.DomainException;

/** The buyer. Comes from the identity provider (Keycloak subject); Ordering has no Customer entity of its own. */
public record CustomerId(String value) {
    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new DomainException("Customer id is required");
        }
    }
}
