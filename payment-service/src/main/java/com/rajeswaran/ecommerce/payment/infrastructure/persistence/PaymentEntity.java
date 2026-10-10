package com.rajeswaran.ecommerce.payment.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment")
class PaymentEntity {
    @Id
    UUID id;
    UUID orderId;
    String customerId;
    BigDecimal amount;
    String currency;
    String status;
    String detail;
    Instant createdAt;
    @Version
    long version;
}
