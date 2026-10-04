CREATE TABLE orders (
    id             UUID           PRIMARY KEY,
    sku            VARCHAR(40)    NOT NULL,
    quantity       INT            NOT NULL CHECK (quantity > 0),
    amount         NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    customer_email VARCHAR(160)   NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    status_reason  VARCHAR(200),
    placed_at      TIMESTAMPTZ    NOT NULL
);

CREATE TABLE stock_items (
    sku       VARCHAR(40) PRIMARY KEY,
    available INT         NOT NULL CHECK (available >= 0),
    reserved  INT         NOT NULL CHECK (reserved >= 0)
);

CREATE TABLE payments (
    order_id   UUID           PRIMARY KEY,
    amount     NUMERIC(12, 2) NOT NULL,
    outcome    VARCHAR(20)    NOT NULL,
    settled_at TIMESTAMPTZ    NOT NULL
);

CREATE TABLE notifications (
    id          BIGSERIAL    PRIMARY KEY,
    order_id    UUID         NOT NULL,
    event       VARCHAR(40)  NOT NULL,
    message     VARCHAR(300) NOT NULL,
    recorded_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_notifications_order ON notifications (order_id, recorded_at);

-- Each module owns its tables, and no foreign key crosses a module boundary: a reference
-- from `payments` to `orders` would make the two impossible to separate later, which is
-- precisely what the module structure is protecting against. The order id is carried as a
-- plain value, the same way it would be between two services.
INSERT INTO stock_items (sku, available, reserved) VALUES
    ('KEYBOARD-01', 25, 0),
    ('MOUSE-02',    40, 0),
    ('MONITOR-03',   3, 0),
    ('LAPTOP-04',   10, 0),
    ('BOOM-99',      5, 0);
