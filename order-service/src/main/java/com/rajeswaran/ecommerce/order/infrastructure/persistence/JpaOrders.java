package com.rajeswaran.ecommerce.order.infrastructure.persistence;

import com.rajeswaran.ecommerce.order.domain.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
    List<OrderEntity> findByCustomerIdOrderByPlacedAtDesc(String customerId);
}

/** Adapter for the Orders port. Order lines are immutable, so they are only written when the order is created. */
@Component
class JpaOrders implements Orders {

    private final OrderJpaRepository repository;

    JpaOrders(OrderJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return repository.findById(id.value()).map(JpaOrders::toDomain);
    }

    @Override
    public List<Order> findByCustomer(CustomerId customerId) {
        return repository.findByCustomerIdOrderByPlacedAtDesc(customerId.value()).stream().map(JpaOrders::toDomain).toList();
    }

    @Override
    public List<Order> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "placedAt")).stream().map(JpaOrders::toDomain).toList();
    }

    @Override
    public void save(Order order) {
        OrderEntity e = repository.findById(order.id().value()).orElseGet(() -> {
            var created = new OrderEntity();
            created.id = order.id().value();
            created.customerId = order.customerId().value();
            created.placedAt = order.placedAt();
            created.lines = new ArrayList<>(order.lines().stream().map(l -> {
                var line = new OrderEntity.Line();
                line.sku = l.sku();
                line.productName = l.productName();
                line.quantity = l.quantity();
                line.unitPrice = l.unitPrice().amount();
                line.currency = l.unitPrice().currency();
                return line;
            }).toList());
            return created;
        });
        e.status = order.status().name();
        e.statusReason = order.statusReason();
        e.cancellationRequested = order.cancellationRequested();
        repository.save(e);
    }

    private static Order toDomain(OrderEntity e) {
        return new Order(new OrderId(e.id), new CustomerId(e.customerId),
                e.lines.stream().map(l -> new OrderLine(l.sku, l.productName, l.quantity, new Money(l.unitPrice, l.currency))).toList(),
                e.placedAt, OrderStatus.valueOf(e.status), e.cancellationRequested, e.statusReason);
    }
}
