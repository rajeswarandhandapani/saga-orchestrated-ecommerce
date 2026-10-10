package com.rajeswaran.ecommerce.platform.messaging;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Records processed message ids. Must run in the same transaction as the handling of the message. */
public class InboxGuard {

    private final JdbcTemplate jdbc;

    public InboxGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @return true if this message id has not been processed before (and claims it). */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean firstTime(UUID messageId, String messageType) {
        return jdbc.update("""
                INSERT INTO processed_message (message_id, message_type) VALUES (?, ?)
                ON CONFLICT (message_id) DO NOTHING""", messageId, messageType) == 1;
    }
}
