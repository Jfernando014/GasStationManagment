CREATE TABLE product_stock_movement
(
    id            BIGSERIAL PRIMARY KEY,
    product_id    BIGINT      NOT NULL REFERENCES product (id),
    journal_date  DATE        NOT NULL,
    location      VARCHAR(10) NOT NULL,
    movement_type VARCHAR(12) NOT NULL,
    quantity      INTEGER     NOT NULL,
    note          VARCHAR(200),
    created_at    TIMESTAMP   NOT NULL,
    CONSTRAINT ck_product_stock_movement_location
        CHECK (location IN ('OFFICE', 'COUNTER')),
    CONSTRAINT ck_product_stock_movement_type
        CHECK (movement_type IN ('INITIAL', 'ENTRY', 'TRANSFER', 'ADJUSTMENT', 'SALE')),
    CONSTRAINT ck_product_stock_movement_quantity CHECK (
        (movement_type = 'INITIAL' AND quantity >= 0)
            OR (movement_type = 'ENTRY' AND quantity > 0)
            OR (movement_type IN ('TRANSFER', 'ADJUSTMENT') AND quantity <> 0)
            OR (movement_type = 'SALE' AND quantity < 0))
);

CREATE INDEX idx_product_stock_movement_product_date ON product_stock_movement (product_id, journal_date);
CREATE INDEX idx_product_stock_movement_date ON product_stock_movement (journal_date);

-- One opening stock per product and location, and one sales row per product and day
CREATE UNIQUE INDEX uq_product_stock_movement_initial
    ON product_stock_movement (product_id, location) WHERE movement_type = 'INITIAL';
CREATE UNIQUE INDEX uq_product_stock_movement_sale
    ON product_stock_movement (product_id, journal_date) WHERE movement_type = 'SALE';