-- Vendor shift closes and product breakdowns (Step 2 of daily journal, sales module).
-- Captures individual seller deliveries, voucher breakdowns, Dominus tirilla readings, and discrepancies.

CREATE TABLE vendor_shift_closes (
    id                   UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_date         DATE           NOT NULL,
    vendor_document      VARCHAR(30)    NOT NULL,
    vendor_name          VARCHAR(150)   NOT NULL,
    shift_name           VARCHAR(50)    NOT NULL,
    pump_number          INTEGER,
    cash                 NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    ga                   NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    credibanco           NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    qr                   NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    redemptions          NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    total_delivered      NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    gallons_motor        NUMERIC(12, 3) NOT NULL DEFAULT 0.000,
    gallons_diesel       NUMERIC(12, 3) NOT NULL DEFAULT 0.000,
    gallons_extra        NUMERIC(12, 3) NOT NULL DEFAULT 0.000,
    gallons_maxpro       NUMERIC(12, 3) NOT NULL DEFAULT 0.000,
    basket_dominus_amount NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    dominus_tirilla      NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    real_discrepancy     NUMERIC(14, 2) NOT NULL DEFAULT 0.00,
    applied_tolerance    NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    result_status        VARCHAR(20)    NOT NULL
        CONSTRAINT ck_vendor_shift_close_status CHECK (result_status IN ('CUADRA', 'SOBRA', 'FALTA')),
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ,

    CONSTRAINT uq_vendor_shift_close UNIQUE (journal_date, vendor_document, shift_name)
);

CREATE INDEX ix_vendor_shift_closes_date ON vendor_shift_closes (journal_date);

CREATE TABLE vendor_close_products (
    id                     UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_shift_close_id  UUID           NOT NULL CONSTRAINT fk_vendor_close_product REFERENCES vendor_shift_closes (id) ON DELETE CASCADE,
    sku                    VARCHAR(50)    NOT NULL,
    product_name           VARCHAR(120)   NOT NULL,
    category               VARCHAR(50)    NOT NULL,
    units_sold             INTEGER        NOT NULL DEFAULT 0,
    unit_price             NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    total_amount           NUMERIC(14, 2) NOT NULL DEFAULT 0.00
);

CREATE INDEX ix_vendor_close_products_close_id ON vendor_close_products (vendor_shift_close_id);
