package com.rajeswaran.ecommerce.payment.application;

import com.rajeswaran.ecommerce.payment.domain.Payment;
import com.rajeswaran.ecommerce.payment.domain.Payments;
import com.rajeswaran.ecommerce.platform.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PaymentQueries {

    public record PaymentView(UUID paymentId, UUID orderId, String customerId, BigDecimal amount, String currency,
                              String status, String detail, Instant createdAt) {
        static PaymentView of(Payment p) {
            return new PaymentView(p.id(), p.orderId(), p.customerId(), p.amount().amount(), p.amount().currency(),
                    p.status().name(), p.detail(), p.createdAt());
        }
    }

    private final Payments payments;

    public PaymentQueries(Payments payments) {
        this.payments = payments;
    }

    public List<PaymentView> all() {
        return payments.findAll().stream().map(PaymentView::of).toList();
    }

    public List<PaymentView> ofCustomer(String customerId) {
        return payments.findByCustomerId(customerId).stream().map(PaymentView::of).toList();
    }

    public PaymentView forOrder(UUID orderId) {
        return payments.findByOrderId(orderId).map(PaymentView::of)
                .orElseThrow(() -> new NotFoundException("No payment for order " + orderId));
    }
}
