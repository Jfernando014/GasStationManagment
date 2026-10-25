-- HU-34 (Credibanco) and HU-35 (QR / Puntos Colombia) + sales parameters with effective date.
-- Parameters now belong to Ventas: daily savings and Credibanco commission (no shared parameter table).

CREATE TABLE sales_parameter (
    id              BIGSERIAL     PRIMARY KEY,
    parameter_key   VARCHAR(40)   NOT NULL,
    numeric_value   NUMERIC(18,6) NOT NULL,
    valid_from      DATE          NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_sales_parameter UNIQUE (parameter_key, valid_from),
    CONSTRAINT ck_sales_parameter_key CHECK (parameter_key IN ('DAILY_SAVINGS', 'CREDIBANCO_COMMISSION_RATE')),
    CONSTRAINT ck_sales_parameter_value CHECK (numeric_value >= 0),
    -- the commission is stored as a fraction: 0.026500 = 2,65 %
    CONSTRAINT ck_sales_parameter_rate CHECK (parameter_key <> 'CREDIBANCO_COMMISSION_RATE' OR numeric_value <= 1)
);

-- Initial values agreed with the team.
INSERT INTO sales_parameter (parameter_key, numeric_value, valid_from) VALUES
    ('DAILY_SAVINGS', 250000, DATE '2026-01-01'),
    ('CREDIBANCO_COMMISSION_RATE', 0.026500, DATE '2026-01-01');

-- One row per vendor and day: datafono vouchers by franchise (HU-34) and the figures of his planilla.
-- "dawn" columns are vouchers printed after midnight (madrugada).
CREATE TABLE credibanco_vendor_day (
    id                    BIGSERIAL     PRIMARY KEY,
    business_date         DATE          NOT NULL,
    worker_id             UUID          NOT NULL CONSTRAINT fk_credibanco_vendor_day_worker REFERENCES worker (id),
    shift_code            VARCHAR(40),
    mastercard            NUMERIC(14,2) NOT NULL DEFAULT 0,
    visa                  NUMERIC(14,2) NOT NULL DEFAULT 0,
    amex                  NUMERIC(14,2) NOT NULL DEFAULT 0,
    mastercard_dawn       NUMERIC(14,2) NOT NULL DEFAULT 0,
    visa_dawn             NUMERIC(14,2) NOT NULL DEFAULT 0,
    amex_dawn             NUMERIC(14,2) NOT NULL DEFAULT 0,
    planilla_credibanco   NUMERIC(14,2) NOT NULL DEFAULT 0,
    planilla_qr           NUMERIC(14,2) NOT NULL DEFAULT 0,
    planilla_redemptions  NUMERIC(14,2) NOT NULL DEFAULT 0,
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_credibanco_vendor_day UNIQUE (business_date, worker_id),
    CONSTRAINT ck_credibanco_vendor_day_amounts CHECK (
        mastercard >= 0 AND visa >= 0 AND amex >= 0 AND mastercard_dawn >= 0 AND visa_dawn >= 0
        AND amex_dawn >= 0 AND planilla_credibanco >= 0 AND planilla_qr >= 0 AND planilla_redemptions >= 0)
);

-- "VALORES MES ANTERIOR": opening figures used only while the previous month has no daily rows in the system.
CREATE TABLE credibanco_previous_month (
    month_start  DATE          PRIMARY KEY,
    mastercard   NUMERIC(14,2) NOT NULL DEFAULT 0,
    visa         NUMERIC(14,2) NOT NULL DEFAULT 0,
    amex         NUMERIC(14,2) NOT NULL DEFAULT 0,
    qr           NUMERIC(14,2) NOT NULL DEFAULT 0,
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_credibanco_previous_month_start CHECK (EXTRACT(DAY FROM month_start) = 1),
    CONSTRAINT ck_credibanco_previous_month_amounts CHECK (mastercard >= 0 AND visa >= 0 AND amex >= 0 AND qr >= 0)
);

-- Rows of the Redeban CSV already attributed to a vendor by shift. Re-importing a date replaces its rows.
CREATE TABLE redeban_qr_transaction (
    id                 BIGSERIAL     PRIMARY KEY,
    business_date      DATE          NOT NULL,
    tx_time            TIME,
    amount             NUMERIC(14,2) NOT NULL,
    reference          VARCHAR(100),
    source_row         INTEGER       NOT NULL,
    worker_id          UUID          CONSTRAINT fk_redeban_qr_worker REFERENCES worker (id),
    assignment_status  VARCHAR(12)   NOT NULL,
    CONSTRAINT ck_redeban_qr_amount CHECK (amount > 0),
    CONSTRAINT ck_redeban_qr_status CHECK (assignment_status IN ('ASSIGNED', 'NO_TIME', 'NO_SHIFT', 'AMBIGUOUS')),
    CONSTRAINT ck_redeban_qr_worker CHECK ((assignment_status = 'ASSIGNED') = (worker_id IS NOT NULL))
);
CREATE INDEX idx_redeban_qr_date ON redeban_qr_transaction (business_date);

-- Day total of Puntos Colombia. Not needed when every planilla has 0 redemptions (the step completes with 0).
CREATE TABLE points_colombia_day (
    business_date  DATE          PRIMARY KEY,
    points_total   NUMERIC(14,2) NOT NULL,
    source         VARCHAR(10)   NOT NULL,
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_points_colombia_total CHECK (points_total >= 0),
    CONSTRAINT ck_points_colombia_source CHECK (source IN ('MANUAL', 'EXCEL'))
);
