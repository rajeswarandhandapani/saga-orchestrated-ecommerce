package com.rajeswaran.ecommerce.payment.domain;

import com.rajeswaran.ecommerce.platform.domain.AggregateRoot;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate: one payment attempt for one order.
 * <pre>authorize ─approved→ AUTHORIZED ─refund→ REFUNDED
 * authorize ─declined→ DECLINED</pre>
 * In the Payments context an "order" is just a reference (orderId) and an amount to charge.
 */
public class Payment extends AggregateRoot<UUID> {

    private final UUID id;
    private final UUID orderId;
    private final String customerId;
    private final Money amount;
    private PaymentStatus status;
    private String detail;
    private final Instant createdAt;

    public Payment(UUID id, UUID orderId, String customerId, Money amount, PaymentStatus status, String detail,
                   Instant createdAt) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.status = status;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public static Payment authorize(UUID orderId, String customerId, Money amount, PaymentGateway gateway) {
        var payment = new Payment(UUID.randomUUID(), orderId, customerId, amount, null, null, Instant.now());
        switch (gateway.authorize(customerId, amount)) {
            case PaymentGateway.Approved approved -> {
                payment.status = PaymentStatus.AUTHORIZED;
                payment.detail = "auth " + approved.authorizationCode();
                payment.raise(new PaymentEvents.PaymentAuthorized(payment.id, orderId));
            }
            case PaymentGateway.Declined declined -> {
                payment.status = PaymentStatus.DECLINED;
                payment.detail = declined.reason();
                payment.raise(new PaymentEvents.PaymentDeclined(payment.id, orderId, declined.reason()));
            }
        }
        return payment;
    }

    /** Compensation. Refunding twice, or refunding a declined payment, changes nothing but still acknowledges. */
    public void refund(String reason) {
        if (status == PaymentStatus.AUTHORIZED) {
            status = PaymentStatus.REFUNDED;
            detail = "refunded: " + reason;
        }
        raise(new PaymentEvents.PaymentRefunded(id, orderId));
    }

    /** Re-announce the current outcome (used when the same AuthorizePayment command arrives again). */
    public void reannounce() {
        switch (status) {
            case AUTHORIZED -> raise(new PaymentEvents.PaymentAuthorized(id, orderId));
            case DECLINED -> raise(new PaymentEvents.PaymentDeclined(id, orderId, detail));
            case REFUNDED -> raise(new PaymentEvents.PaymentDeclined(id, orderId, "Payment already refunded"));
        }
    }

    @Override
    public UUID id() {
        return id;
    }

    public UUID orderId() {
        return orderId;
    }

    public String customerId() {
        return customerId;
    }

    public Money amount() {
        return amount;
    }

    public PaymentStatus status() {
        return status;
    }

    public String detail() {
        return detail;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
