package com.rajeswaran.ecommerce.payment.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository port. */
public interface Payments {
    Optional<Payment> findByOrderId(UUID orderId);

    List<Payment> findAll();

    List<Payment> findByCustomerId(String customerId);

    void save(Payment payment);
}
