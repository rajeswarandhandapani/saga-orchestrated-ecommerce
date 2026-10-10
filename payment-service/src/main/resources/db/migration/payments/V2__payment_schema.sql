CREATE TABLE payment (
    id          UUID PRIMARY KEY,
    order_id    UUID           NOT NULL UNIQUE,  -- one payment per order in this model
    customer_id VARCHAR(100)   NOT NULL,
    amount      NUMERIC(12, 2) NOT NULL,
    currency    VARCHAR(3)     NOT NULL,
    status      VARCHAR(20)    NOT NULL,
    detail      VARCHAR(500),
    created_at  TIMESTAMPTZ    NOT NULL,
    version     BIGINT         NOT NULL DEFAULT 0
);
CREATE INDEX ix_payment_customer ON payment (customer_id);
