CREATE TABLE saga_instance (
    id              UUID PRIMARY KEY,
    order_id        UUID           NOT NULL UNIQUE,
    customer_id     VARCHAR(100)   NOT NULL,
    total           NUMERIC(12, 2) NOT NULL,
    currency        VARCHAR(3)     NOT NULL,
    state           VARCHAR(30)    NOT NULL,
    abort_requested BOOLEAN        NOT NULL DEFAULT FALSE,
    abort_reason    VARCHAR(500),
    failure_reason  VARCHAR(500),
    started_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL,
    version         BIGINT         NOT NULL DEFAULT 0
);
CREATE INDEX ix_saga_state ON saga_instance (state, updated_at);

CREATE TABLE saga_line (
    saga_id  UUID        NOT NULL REFERENCES saga_instance (id),
    line_no  INT         NOT NULL,
    sku      VARCHAR(40) NOT NULL,
    quantity INT         NOT NULL,
    PRIMARY KEY (saga_id, line_no)
);

CREATE TABLE saga_step (
    saga_id      UUID         NOT NULL REFERENCES saga_instance (id),
    step_no      INT          NOT NULL,
    at           TIMESTAMPTZ  NOT NULL,
    kind         VARCHAR(20)  NOT NULL,
    message      VARCHAR(100) NOT NULL,
    detail       VARCHAR(500),
    compensation BOOLEAN      NOT NULL,
    PRIMARY KEY (saga_id, step_no)
);
