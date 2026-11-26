CREATE TABLE fuel_purchase_price
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fuel_type        VARCHAR(20)    NOT NULL,
    price_per_gallon NUMERIC(12, 2) NOT NULL,
    valid_from       DATE           NOT NULL,
    CONSTRAINT uq_fuel_purchase_price_fuel_type_valid_from
        UNIQUE (fuel_type, valid_from),
    CONSTRAINT ck_fuel_purchase_price_fuel_type
        CHECK (fuel_type IN ('MOTOR', 'DIESEL', 'EXTRA', 'MAX_PRO')),
    CONSTRAINT ck_fuel_purchase_price_positive CHECK (price_per_gallon > 0)
);
