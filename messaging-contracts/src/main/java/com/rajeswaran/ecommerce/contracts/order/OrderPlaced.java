package com.rajeswaran.ecommerce.contracts.order;

import com.rajeswaran.ecommerce.contracts.LineItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Integration event that starts the PlaceOrder saga. */
public record OrderPlaced(UUID orderId, String customerId, List<LineItem> lines, BigDecimal total, String currency)
        implements OrderEvent {
}
