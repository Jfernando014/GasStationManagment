CREATE TABLE product
(
    id       BIGSERIAL PRIMARY KEY,
    code     VARCHAR(20)    NOT NULL,
    name     VARCHAR(100)   NOT NULL,
    category VARCHAR(20)    NOT NULL,
    price    NUMERIC(12, 2) NOT NULL,
    cost     NUMERIC(12, 2),
    active   BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_product_code UNIQUE (code),
    CONSTRAINT ck_product_category CHECK (category IN ('LUBRICANTE', 'ADITIVO', 'REFRIGERANTE', 'OTRO')),
    CONSTRAINT ck_product_price CHECK (price > 0),
    CONSTRAINT ck_product_cost CHECK (cost IS NULL OR cost >= 0)
);