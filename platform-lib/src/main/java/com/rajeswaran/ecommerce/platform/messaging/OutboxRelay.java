package com.rajeswaran.ecommerce.platform.messaging;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Polls the outbox and publishes pending rows to Kafka in creation order.
 * <p>
 * {@code FOR UPDATE SKIP LOCKED} lets several instances of a service run relays without publishing a row twice.
 * Delivery is at-least-once (a crash after send but before commit re-sends), which is why consumers use the inbox.
 */
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate tx;

    public OutboxRelay(JdbcTemplate jdbc, KafkaTemplate<String, String> kafka, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        this.tx = tx;
    }

    private record Row(UUID id, String topic, String key, String type, String payload, String correlationId) {
    }

    @Scheduled(fixedDelayString = "${platform.outbox.poll-interval:500ms}")
    public void publishPending() {
        tx.executeWithoutResult(status -> {
            var rows = jdbc.query("""
                            SELECT id, topic, message_key, message_type, payload, correlation_id
                            FROM outbox_message WHERE published_at IS NULL
                            ORDER BY created_at LIMIT 100 FOR UPDATE SKIP LOCKED""",
                    (rs, i) -> new Row(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5), rs.getString(6)));
            for (Row row : rows) {
                try {
                    send(row);
                    jdbc.update("UPDATE outbox_message SET published_at = now() WHERE id = ?", row.id());
                } catch (Exception e) {
                    // Stop here so later messages for the same order are not published ahead of this one.
                    log.warn("Outbox publish failed for {} {} - will retry: {}", row.type(), row.id(), e.getMessage());
                    break;
                }
            }
        });
    }

    private void send(Row row) throws Exception {
        var record = new ProducerRecord<String, String>(row.topic(), row.key(), row.payload());
        record.headers().add(MessageHeaders.MESSAGE_ID, bytes(row.id().toString()));
        record.headers().add(MessageHeaders.MESSAGE_TYPE, bytes(row.type()));
        if (row.correlationId() != null) {
            record.headers().add(MessageHeaders.CORRELATION_ID, bytes(row.correlationId()));
        }
        kafka.send(record).get(10, TimeUnit.SECONDS);
        log.info("Published {} key={} to {}", row.type(), row.key(), row.topic());
    }

    /** Housekeeping: published rows are only kept for a while for debugging. */
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    public void purgePublished() {
        jdbc.update("DELETE FROM outbox_message WHERE published_at < now() - make_interval(secs => ?)",
                Duration.ofDays(1).toSeconds());
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
