package com.rajeswaran.ecommerce.order.domain;

import java.util.UUID;

public record OrderId(UUID value) {
    public static OrderId newId() {
        return new OrderId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
