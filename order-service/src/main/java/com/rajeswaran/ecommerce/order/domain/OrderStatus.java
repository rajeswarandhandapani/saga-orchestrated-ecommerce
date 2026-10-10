package com.rajeswaran.ecommerce.order.domain;

public enum OrderStatus {
    /** Placed; the saga is working on it. */
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED;

    public boolean isFinal() {
        return this != PENDING;
    }
}
