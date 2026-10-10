-- Transactional outbox: written in the SAME database transaction as the aggregate change.
-- OutboxRelay later publishes unpublished rows to Kafka. This removes the "dual write" problem
-- (DB commit succeeded but Kafka send failed, or vice versa).
CREATE TABLE outbox_message (
    id             UUID PRIMARY KEY,
    topic          VARCHAR(100)  NOT NULL,
    message_key    VARCHAR(100)  NOT NULL,
    message_type   VARCHAR(100)  NOT NULL,
    payload        TEXT          NOT NULL,
    correlation_id VARCHAR(100),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    published_at   TIMESTAMPTZ
);
CREATE INDEX ix_outbox_unpublished ON outbox_message (created_at) WHERE published_at IS NULL;

-- Idempotent consumer (inbox): Kafka is at-least-once, so the same message can arrive twice.
-- A message id is recorded in the same transaction as the handling; a second delivery is skipped.
CREATE TABLE processed_message (
    message_id   UUID PRIMARY KEY,
    message_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
