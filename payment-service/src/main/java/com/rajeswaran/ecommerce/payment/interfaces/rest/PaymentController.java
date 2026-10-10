package com.rajeswaran.ecommerce.payment.interfaces.rest;

import com.rajeswaran.ecommerce.payment.application.PaymentQueries;
import com.rajeswaran.ecommerce.payment.application.PaymentQueries.PaymentView;
import com.rajeswaran.ecommerce.platform.domain.NotFoundException;
import com.rajeswaran.ecommerce.platform.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Payments are created only by the saga (via Kafka); the REST API is read-only. */
@RestController
@RequestMapping("/api/payments")
class PaymentController {

    private final PaymentQueries queries;

    PaymentController(PaymentQueries queries) {
        this.queries = queries;
    }

    @GetMapping
    List<PaymentView> list() {
        return CurrentUser.isAdmin() ? queries.all() : queries.ofCustomer(CurrentUser.id());
    }

    @GetMapping("/order/{orderId}")
    PaymentView forOrder(@PathVariable UUID orderId) {
        PaymentView view = queries.forOrder(orderId);
        if (!CurrentUser.isAdmin() && !view.customerId().equals(CurrentUser.id())) {
            throw new NotFoundException("No payment for order " + orderId); // don't leak existence
        }
        return view;
    }
}
