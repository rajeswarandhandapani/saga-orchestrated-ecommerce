package com.rajeswaran.ecommerce.contracts.payment;

import java.util.UUID;

/** Compensation for an authorized payment. Must be safe to receive when nothing was charged. */
public record RefundPayment(UUID orderId, String reason) implements PaymentCommand {
}
