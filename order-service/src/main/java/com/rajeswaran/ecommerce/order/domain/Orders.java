package com.rajeswaran.ecommerce.order.domain;

import java.util.List;
import java.util.Optional;

/** Repository port. */
public interface Orders {
    Optional<Order> findById(OrderId id);

    List<Order> findByCustomer(CustomerId customerId);

    List<Order> findAll();

    void save(Order order);
}
