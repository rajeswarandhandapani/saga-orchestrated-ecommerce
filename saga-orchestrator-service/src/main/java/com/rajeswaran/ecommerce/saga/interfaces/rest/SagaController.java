package com.rajeswaran.ecommerce.saga.interfaces.rest;

import com.rajeswaran.ecommerce.platform.domain.NotFoundException;
import com.rajeswaran.ecommerce.platform.security.CurrentUser;
import com.rajeswaran.ecommerce.saga.application.SagaQueries;
import com.rajeswaran.ecommerce.saga.application.SagaQueries.SagaView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sagas")
class SagaController {

    private final SagaQueries queries;

    SagaController(SagaQueries queries) {
        this.queries = queries;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    List<SagaView> recent() {
        return queries.recent();
    }

    /** Timeline for one order; customers may only see their own. */
    @GetMapping("/order/{orderId}")
    SagaView forOrder(@PathVariable UUID orderId) {
        SagaView view = queries.forOrder(orderId);
        if (!CurrentUser.isAdmin() && !view.customerId().equals(CurrentUser.id())) {
            throw new NotFoundException("No saga for order " + orderId);
        }
        return view;
    }
}
