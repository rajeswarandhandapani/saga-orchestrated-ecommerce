package com.rajeswaran.ecommerce.inventory.infrastructure.persistence;

import com.rajeswaran.ecommerce.inventory.domain.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Component
class JpaReservations implements Reservations {

    private final ReservationJpaRepository reservations;

    JpaReservations(ReservationJpaRepository reservations) {
        this.reservations = reservations;
    }

    @Override
    public Optional<Reservation> findByOrderId(UUID orderId) {
        return reservations.findById(orderId).map(e -> new Reservation(e.orderId,
                e.lines.stream().map(l -> new ReservationLine(new Sku(l.sku), l.quantity)).toList(),
                ReservationStatus.valueOf(e.status)));
    }

    @Override
    public void save(Reservation reservation) {
        ReservationEntity e = reservations.findById(reservation.id()).orElseGet(() -> {
            var created = new ReservationEntity();
            created.orderId = reservation.id();
            created.lines = new ArrayList<>(reservation.lines().stream().map(l -> {
                var line = new ReservationEntity.Line();
                line.sku = l.sku().value();
                line.quantity = l.quantity();
                return line;
            }).toList());
            return created;
        });
        e.status = reservation.status().name();
        e.updatedAt = Instant.now();
        reservations.save(e);
    }
}
