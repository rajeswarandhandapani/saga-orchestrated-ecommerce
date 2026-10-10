package com.rajeswaran.ecommerce.contracts.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record AuthorizePayment(UUID orderId, String customerId, BigDecimal amount, String currency) implements PaymentCommand {
}
