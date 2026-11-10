-- SCRUM-107: immutable seller/shift sales snapshots and recoverable processing per closed operational date.
-- No foreign keys to other modules. Their identities and metadata are historical values, not live associations.
CREATE TABLE statistics_journal_sales (
    journal_date DATE PRIMARY KEY,
    journal_id BIGINT CHECK (journal_id > 0),
    consecutive BIGINT NOT NULL CHECK (consecutive > 0),
    -- Keep whole seconds and nanos separately to retain the event's LocalDateTime without microsecond rounding.
    closed_at TIMESTAMP NOT NULL,
    closed_at_nano INTEGER NOT NULL CHECK (closed_at_nano BETWEEN 0 AND 999999999),
    status VARCHAR(12) NOT NULL CHECK (status IN ('PENDING', 'PROCESSED')),
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    sale_count INTEGER,
    last_error VARCHAR(500),
    processed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_statistics_journal_seconds CHECK (closed_at = date_trunc('second', closed_at)),
    CONSTRAINT ck_statistics_journal_processing CHECK (
        (status = 'PENDING' AND sale_count IS NULL AND processed_at IS NULL)
        OR (status = 'PROCESSED' AND attempts > 0 AND sale_count >= 0 AND sale_count IS NOT NULL
            AND processed_at IS NOT NULL AND last_error IS NULL)
    )
);

CREATE TABLE statistics_shift_sale (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    journal_date DATE NOT NULL REFERENCES statistics_journal_sales (journal_date),
    -- Supports original documents and UUID strings. Source V22's VARCHAR(30) incompatibility remains unresolved.
    vendor_document TEXT NOT NULL CHECK (vendor_document ~ '[^[:space:]]'),
    shift_name TEXT NOT NULL CHECK (shift_name ~ '[^[:space:]]'),
    worker_id UUID NOT NULL CHECK (worker_id <> '00000000-0000-0000-0000-000000000000'::uuid),
    vendor_name TEXT NOT NULL CHECK (vendor_name ~ '[^[:space:]]'),
    worked_code TEXT NOT NULL CHECK (worked_code ~ '[^[:space:]]'),
    effective_role VARCHAR(10) NOT NULL,
    support BOOLEAN NOT NULL,
    motor_gallons NUMERIC NOT NULL,
    motor_gallons_scale INTEGER NOT NULL,
    diesel_gallons NUMERIC NOT NULL,
    diesel_gallons_scale INTEGER NOT NULL,
    extra_gallons NUMERIC NOT NULL,
    extra_gallons_scale INTEGER NOT NULL,
    maxpro_gallons NUMERIC NOT NULL,
    maxpro_gallons_scale INTEGER NOT NULL,
    basket_dominus_amount_cop NUMERIC NOT NULL,
    basket_dominus_amount_cop_scale INTEGER NOT NULL,
    dominus_tirilla_cop NUMERIC NOT NULL,
    dominus_tirilla_cop_scale INTEGER NOT NULL,
    total_delivered_cop NUMERIC NOT NULL,
    total_delivered_cop_scale INTEGER NOT NULL,
    CONSTRAINT uq_statistics_shift_sale_identity UNIQUE (journal_date, vendor_document, shift_name),
    CONSTRAINT ck_statistics_shift_sale_role CHECK (
        effective_role IN ('TITULAR', 'APOYO') AND support = (effective_role = 'APOYO')
    ),
    -- NUMERIC has no fixed typmod: it never coerces input to a monetary or gallon scale.
    -- Original BigDecimal scales also retain negative scales and scientific-notation zero values.
    CONSTRAINT ck_statistics_motor_gallons CHECK (
        motor_gallons NOT IN ('NaN'::numeric, 'Infinity'::numeric, '-Infinity'::numeric)
        AND motor_gallons_scale BETWEEN -131071 AND 16383
        AND motor_gallons = trunc(motor_gallons, motor_gallons_scale)
    ),
    CONSTRAINT ck_statistics_diesel_gallons CHECK (
        diesel_gallons NOT IN ('NaN'::numeric, 'Infinity'::numeric, '-Infinity'::numeric)
        AND diesel_gallons_scale BETWEEN -131071 AND 16383
        AND diesel_gallons = trunc(diesel_gallons, diesel_gallons_scale)
    ),
    CONSTRAINT ck_statistics_extra_gallons CHECK (
        extra_gallons NOT IN ('NaN'::numeric, 'Infinity'::numeric, '-Infinity'::numeric)
        AND extra_gallons_scale BETWEEN -131071 AND 16383
        AND extra_gallons = trunc(extra_gallons, extra_gallons_scale)
    ),
    CONSTRAINT ck_statistics_maxpro_gallons CHECK (
        maxpro_gallons NOT IN ('NaN'::numeric, 'Infinity'::numeric, '-Infinity'::numeric)
        AND maxpro_gallons_scale BETWEEN -131071 AND 16383
        AND maxpro_gallons = trunc(maxpro_gallons, maxpro_gallons_scale)
    ),
    CONSTRAINT ck_statistics_basket_dominus_amount_cop CHECK (
        basket_dominus_amount_cop NOT IN ('NaN'::numeric, 'Infinity'::numeric, '-Infinity'::numeric)
        AND basket_dominus_amount_cop_scale BETWEEN -131071 AND 16383
        AND basket_dominus_amount_cop = trunc(basket_dominus_amount_cop, basket_dominus_amount_cop_scale)
    ),
    CONSTRAINT ck_statistics_dominus_tirilla_cop CHECK (
        dominus_tirilla_cop NOT IN ('NaN'::numeric, 'Infinity'::numeric, '-Infinity'::numeric)
        AND dominus_tirilla_cop_scale BETWEEN -131071 AND 16383
        AND dominus_tirilla_cop = trunc(dominus_tirilla_cop, dominus_tirilla_cop_scale)
    ),
    CONSTRAINT ck_statistics_total_delivered_cop CHECK (
        total_delivered_cop NOT IN ('NaN'::numeric, 'Infinity'::numeric, '-Infinity'::numeric)
        AND total_delivered_cop_scale BETWEEN -131071 AND 16383
        AND total_delivered_cop = trunc(total_delivered_cop, total_delivered_cop_scale)
    )
);

CREATE INDEX ix_statistics_journal_sales_pending
    ON statistics_journal_sales (journal_date) WHERE status = 'PENDING';
