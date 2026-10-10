package com.rajeswaran.ecommerce.contracts.payment;

import com.rajeswaran.ecommerce.contracts.Event;

public sealed interface PaymentEvent extends Event permits PaymentAuthorized, PaymentDeclined, PaymentRefunded {
}
