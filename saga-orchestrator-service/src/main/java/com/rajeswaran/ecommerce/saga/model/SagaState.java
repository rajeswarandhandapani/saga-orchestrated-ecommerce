package com.rajeswaran.ecommerce.saga.model;

public enum SagaState {
    AWAITING_RESERVATION,
    AWAITING_PAYMENT,
    /** compensating: waiting for Payments to refund */
    AWAITING_REFUND,
    /** compensating: waiting for Inventory to release stock */
    AWAITING_RELEASE,

    /** happy path finished */
    COMPLETED,
    /** failed before any side effect needed undoing (e.g. out of stock) */
    REJECTED,
    /** failed after side effects; they were undone */
    COMPENSATED,
    /** aborted on request (customer cancel or timeout); side effects undone */
    CANCELLED;

    public boolean isFinal() {
        return ordinal() >= COMPLETED.ordinal();
    }
}
