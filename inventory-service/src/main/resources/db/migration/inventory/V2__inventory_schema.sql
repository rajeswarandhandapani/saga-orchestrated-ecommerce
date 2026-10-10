CREATE TABLE product (
    sku         VARCHAR(40) PRIMARY KEY,
    name        VARCHAR(200)   NOT NULL,
    description VARCHAR(1000),
    price       NUMERIC(12, 2) NOT NULL,
    currency    VARCHAR(3)     NOT NULL
);

CREATE TABLE stock_item (
    sku      VARCHAR(40) PRIMARY KEY REFERENCES product (sku),
    on_hand  INT    NOT NULL CHECK (on_hand >= 0),
    reserved INT    NOT NULL CHECK (reserved >= 0 AND reserved <= on_hand),
    version  BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE reservation (
    order_id   UUID PRIMARY KEY,
    status     VARCHAR(20) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE reservation_line (
    order_id UUID        NOT NULL REFERENCES reservation (order_id),
    sku      VARCHAR(40) NOT NULL,
    quantity INT         NOT NULL
);
