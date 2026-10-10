package com.rajeswaran.ecommerce.platform.messaging;

import com.rajeswaran.ecommerce.contracts.Message;

import java.util.UUID;

/** A decoded Kafka record: the contract payload plus the envelope metadata from headers. */
public record IncomingMessage(UUID messageId, String type, String correlationId, Message payload) {
}
