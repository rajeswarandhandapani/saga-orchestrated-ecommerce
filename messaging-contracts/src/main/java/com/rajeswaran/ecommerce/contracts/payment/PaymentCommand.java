package com.rajeswaran.ecommerce.contracts.payment;

import com.rajeswaran.ecommerce.contracts.Command;

public sealed interface PaymentCommand extends Command permits AuthorizePayment, RefundPayment {
}
