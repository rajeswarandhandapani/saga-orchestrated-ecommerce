package com.rajeswaran.ecommerce.payment.application;

import com.rajeswaran.ecommerce.contracts.Topics;
import com.rajeswaran.ecommerce.contracts.payment.*;
import com.rajeswaran.ecommerce.payment.domain.*;
import com.rajeswaran.ecommerce.platform.domain.DomainEvent;
import com.rajeswaran.ecommerce.platform.messaging.OutboxPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PaymentCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(PaymentCommandHandler.class);

    private final Payments payments;
    private final PaymentGateway gateway;
    private final OutboxPublisher outbox;

    public PaymentCommandHandler(Payments payments, PaymentGateway gateway, OutboxPublisher outbox) {
        this.payments = payments;
        this.gateway = gateway;
        this.outbox = outbox;
    }

    public void handle(PaymentCommand command) {
        switch (command) {
            case AuthorizePayment c -> authorize(c);
            case RefundPayment c -> refund(c);
        }
    }

    void authorize(AuthorizePayment command) {
        Payment payment = payments.findByOrderId(command.orderId()).map(existing -> {
            log.info("Payment for order {} already {} - re-announcing", command.orderId(), existing.status());
            existing.reannounce();
            return existing;
        }).orElseGet(() -> Payment.authorize(command.orderId(), command.customerId(),
                new Money(command.amount(), command.currency()), gateway));
        payments.save(payment);
        publish(payment.pullDomainEvents());
    }

    void refund(RefundPayment command) {
        payments.findByOrderId(command.orderId()).ifPresentOrElse(payment -> {
            payment.refund(command.reason());
            payments.save(payment);
            publish(payment.pullDomainEvents());
        }, () -> {
            log.info("No payment for order {} - acknowledging refund", command.orderId());
            outbox.append(Topics.PAYMENT_EVENTS, new PaymentRefunded(command.orderId()));
        });
    }

    private void publish(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            switch (event) {
                case PaymentEvents.PaymentAuthorized e ->
                        outbox.append(Topics.PAYMENT_EVENTS, new PaymentAuthorized(e.orderId(), e.paymentId()));
                case PaymentEvents.PaymentDeclined e ->
                        outbox.append(Topics.PAYMENT_EVENTS, new PaymentDeclined(e.orderId(), e.reason()));
                case PaymentEvents.PaymentRefunded e ->
                        outbox.append(Topics.PAYMENT_EVENTS, new PaymentRefunded(e.orderId()));
                default -> log.debug("No integration event for {}", event);
            }
        }
    }
}
