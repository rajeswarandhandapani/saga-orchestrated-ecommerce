CREATE TABLE orders (
    id                     UUID PRIMARY KEY,
    customer_id            VARCHAR(100) NOT NULL,
    status                 VARCHAR(20)  NOT NULL,
    status_reason          VARCHAR(500),
    cancellation_requested BOOLEAN      NOT NULL DEFAULT FALSE,
    placed_at              TIMESTAMPTZ  NOT NULL,
    version                BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX ix_orders_customer ON orders (customer_id, placed_at DESC);

-- Order lines are part of the Order aggregate (no identity of their own); price is a snapshot.
CREATE TABLE order_line (
    order_id     UUID           NOT NULL REFERENCES orders (id),
    line_no      INT            NOT NULL,
    sku          VARCHAR(40)    NOT NULL,
    product_name VARCHAR(200)   NOT NULL,
    quantity     INT            NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    currency     VARCHAR(3)     NOT NULL,
    PRIMARY KEY (order_id, line_no)
);
