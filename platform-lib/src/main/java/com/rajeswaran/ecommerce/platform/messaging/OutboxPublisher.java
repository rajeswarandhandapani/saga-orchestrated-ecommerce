package com.rajeswaran.ecommerce.platform.messaging;

import com.rajeswaran.ecommerce.contracts.Message;
import com.rajeswaran.ecommerce.contracts.MessageTypes;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Appends a message to the outbox table. MUST be called inside the business transaction
 * (propagation MANDATORY enforces that) so the aggregate change and the message commit or roll back together.
 */
public class OutboxPublisher {

    private final JdbcTemplate jdbc;
    private final MessageCodec codec;

    public OutboxPublisher(JdbcTemplate jdbc, MessageCodec codec) {
        this.jdbc = jdbc;
        this.codec = codec;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(String topic, Message message) {
        jdbc.update("""
                        INSERT INTO outbox_message (id, topic, message_key, message_type, payload, correlation_id)
                        VALUES (?, ?, ?, ?, ?, ?)""",
                UUID.randomUUID(), topic, message.orderId().toString(), MessageTypes.nameOf(message),
                codec.encode(message), MDC.get(MessageHeaders.CORRELATION_MDC_KEY));
    }
}
