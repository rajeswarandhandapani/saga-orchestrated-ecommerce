package com.rajeswaran.ecommerce.inventory.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reservation")
class ReservationEntity {
    @Id
    UUID orderId;
    String status;
    Instant updatedAt;
    @Version
    long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reservation_line", joinColumns = @JoinColumn(name = "order_id"))
    List<Line> lines = new ArrayList<>();

    @Embeddable
    static class Line {
        String sku;
        int quantity;
    }
}
