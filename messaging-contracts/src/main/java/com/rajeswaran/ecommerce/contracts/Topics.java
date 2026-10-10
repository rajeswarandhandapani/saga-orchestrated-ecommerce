package com.rajeswaran.ecommerce.contracts;

import java.util.List;

/**
 * One command topic and one event topic per bounded context.
 * The owning context is the only consumer of its command topic and the only producer of its event topic.
 */
public final class Topics {
    public static final String ORDER_COMMANDS = "order.commands";
    public static final String ORDER_EVENTS = "order.events";
    public static final String INVENTORY_COMMANDS = "inventory.commands";
    public static final String INVENTORY_EVENTS = "inventory.events";
    public static final String PAYMENT_COMMANDS = "payment.commands";
    public static final String PAYMENT_EVENTS = "payment.events";

    public static final List<String> ALL = List.of(
            ORDER_COMMANDS, ORDER_EVENTS, INVENTORY_COMMANDS, INVENTORY_EVENTS, PAYMENT_COMMANDS, PAYMENT_EVENTS);

    private Topics() {
    }
}
