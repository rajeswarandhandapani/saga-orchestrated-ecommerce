package com.rajeswaran.ecommerce.payment.domain;

/**
 * Domain port to the outside payment provider (Stripe, Adyen ...). The domain decides what to do with the answer;
 * infrastructure decides how to talk to the provider.
 */
public interface PaymentGateway {

    sealed interface Result permits Approved, Declined {
    }

    record Approved(String authorizationCode) implements Result {
    }

    record Declined(String reason) implements Result {
    }

    Result authorize(String customerId, Money amount);
}
