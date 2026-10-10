package com.rajeswaran.ecommerce.order.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
class OrderEntity {
    @Id
    UUID id;
    String customerId;
    String status;
    String statusReason;
    boolean cancellationRequested;
    Instant placedAt;
    @Version
    long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_line", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "line_no")
    List<Line> lines = new ArrayList<>();

    @Embeddable
    static class Line {
        String sku;
        String productName;
        int quantity;
        BigDecimal unitPrice;
        String currency;
    }
}
