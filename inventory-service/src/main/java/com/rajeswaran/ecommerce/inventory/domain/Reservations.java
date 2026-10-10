package com.rajeswaran.ecommerce.inventory.domain;

import java.util.Optional;
import java.util.UUID;

/** Repository port for the Reservation aggregate. */
public interface Reservations {
    Optional<Reservation> findByOrderId(UUID orderId);

    void save(Reservation reservation);
}
