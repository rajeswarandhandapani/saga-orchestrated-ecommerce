package com.rajeswaran.ecommerce.order.interfaces.rest;

import com.rajeswaran.ecommerce.order.application.OrderQueries;
import com.rajeswaran.ecommerce.order.application.OrderQueries.OrderView;
import com.rajeswaran.ecommerce.order.application.OrderingService;
import com.rajeswaran.ecommerce.order.domain.CustomerId;
import com.rajeswaran.ecommerce.order.domain.Order;
import com.rajeswaran.ecommerce.platform.domain.NotFoundException;
import com.rajeswaran.ecommerce.platform.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
class OrderController {

    record LineRequest(@NotBlank String sku, @Min(1) @Max(100) int quantity) {
    }

    record PlaceOrderRequest(@NotEmpty List<@Valid LineRequest> lines) {
    }

    private final OrderingService ordering;
    private final OrderQueries queries;

    OrderController(OrderingService ordering, OrderQueries queries) {
        this.ordering = ordering;
        this.queries = queries;
    }

    /**
     * 202 Accepted: the order exists (PENDING) but its outcome is decided asynchronously by the saga.
     * The client follows the Location header / polls to see CONFIRMED or REJECTED.
     */
    @PostMapping
    ResponseEntity<OrderView> place(@Valid @RequestBody PlaceOrderRequest request) {
        Order order = ordering.placeOrder(new CustomerId(CurrentUser.id()),
                request.lines().stream().map(l -> new OrderingService.RequestedLine(l.sku(), l.quantity())).toList());
        return ResponseEntity.accepted()
                .location(URI.create("/api/orders/" + order.id()))
                .body(OrderView.of(order));
    }

    @GetMapping
    List<OrderView> list(@RequestParam(defaultValue = "mine") String scope) {
        if ("all".equals(scope) && CurrentUser.isAdmin()) {
            return queries.all();
        }
        return queries.ofCustomer(new CustomerId(CurrentUser.id()));
    }

    @GetMapping("/{orderId}")
    OrderView get(@PathVariable UUID orderId) {
        OrderView view = queries.byId(orderId);
        if (!CurrentUser.isAdmin() && !view.customerId().equals(CurrentUser.id())) {
            throw new NotFoundException("No order " + orderId);
        }
        return view;
    }

    @PostMapping("/{orderId}/cancel")
    ResponseEntity<OrderView> cancel(@PathVariable UUID orderId) {
        ordering.requestCancellation(orderId, new CustomerId(CurrentUser.id()));
        return ResponseEntity.accepted().body(queries.byId(orderId));
    }
}
