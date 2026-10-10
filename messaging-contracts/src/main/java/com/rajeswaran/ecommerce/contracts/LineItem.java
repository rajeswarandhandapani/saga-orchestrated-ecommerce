package com.rajeswaran.ecommerce.contracts;

import java.math.BigDecimal;

/** An order line as seen on the wire. unitPrice may be null where the receiver does not need it. */
public record LineItem(String sku, int quantity, BigDecimal unitPrice) {
}
