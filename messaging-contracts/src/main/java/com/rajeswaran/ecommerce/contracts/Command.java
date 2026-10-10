package com.rajeswaran.ecommerce.contracts;

/**
 * Imperative message: "please do X". Sent by the orchestrator to exactly one context.
 * Per-context sub-interfaces (e.g. {@code InventoryCommand}) are sealed so handlers can switch exhaustively.
 */
public non-sealed interface Command extends Message {
}
