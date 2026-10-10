package com.rajeswaran.ecommerce.payment.infrastructure.persistence;

import com.rajeswaran.ecommerce.payment.domain.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {
    Optional<PaymentEntity> findByOrderId(UUID orderId);

    List<PaymentEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}

@Component
class JpaPayments implements Payments {

    private final PaymentJpaRepository repository;

    JpaPayments(PaymentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId).map(JpaPayments::toDomain);
    }

    @Override
    public List<Payment> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream().map(JpaPayments::toDomain).toList();
    }

    @Override
    public List<Payment> findByCustomerId(String customerId) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(JpaPayments::toDomain).toList();
    }

    @Override
    public void save(Payment payment) {
        PaymentEntity e = repository.findById(payment.id()).orElseGet(() -> {
            var created = new PaymentEntity();
            created.id = payment.id();
            created.orderId = payment.orderId();
            created.customerId = payment.customerId();
            created.amount = payment.amount().amount();
            created.currency = payment.amount().currency();
            created.createdAt = payment.createdAt();
            return created;
        });
        e.status = payment.status().name();
        e.detail = payment.detail();
        repository.save(e);
    }

    private static Payment toDomain(PaymentEntity e) {
        return new Payment(e.id, e.orderId, e.customerId, new Money(e.amount, e.currency),
                PaymentStatus.valueOf(e.status), e.detail, e.createdAt);
    }
}
