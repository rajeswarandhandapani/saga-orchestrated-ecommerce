package com.rajeswaran.ecommerce.order.application;

import com.rajeswaran.ecommerce.order.domain.*;
import com.rajeswaran.ecommerce.platform.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderQueries {

    public record LineView(String sku, String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    public record OrderView(UUID orderId, String customerId, String status, String statusReason,
                            boolean cancellationRequested, BigDecimal total, String currency, Instant placedAt,
                            List<LineView> lines) {
        public static OrderView of(Order o) {
            Money total = o.total();
            return new OrderView(o.id().value(), o.customerId().value(), o.status().name(), o.statusReason(),
                    o.cancellationRequested(), total.amount(), total.currency(), o.placedAt(),
                    o.lines().stream().map(l -> new LineView(l.sku(), l.productName(), l.quantity(),
                            l.unitPrice().amount(), l.lineTotal().amount())).toList());
        }
    }

    private final Orders orders;

    public OrderQueries(Orders orders) {
        this.orders = orders;
    }

    public List<OrderView> ofCustomer(CustomerId customerId) {
        return orders.findByCustomer(customerId).stream().map(OrderView::of).toList();
    }

    public List<OrderView> all() {
        return orders.findAll().stream().map(OrderView::of).toList();
    }

    public OrderView byId(UUID orderId) {
        return orders.findById(new OrderId(orderId)).map(OrderView::of)
                .orElseThrow(() -> new NotFoundException("No order " + orderId));
    }
}
