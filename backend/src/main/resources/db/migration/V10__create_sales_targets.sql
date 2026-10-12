-- SCRUM-40 / HU-14: configured aggregate targets, independent of individual calculations.
-- Applications must validate exact numeric representability BEFORE insertion: PostgreSQL
-- coerces NUMERIC(30,12) before CHECK constraints and may otherwise round incoming values.
CREATE TABLE sales_targets (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    target_year INTEGER NOT NULL,
    target_month SMALLINT,
    category VARCHAR(30) NOT NULL,
    target_value NUMERIC(30,12) NOT NULL,
    unit VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_sales_targets_month CHECK (target_month IS NULL OR target_month BETWEEN 1 AND 12),
    CONSTRAINT ck_sales_targets_value CHECK (target_value >= 0 AND target_value <> 'NaN'::numeric),
    CONSTRAINT ck_sales_targets_category_unit CHECK (
        (category = 'FUEL_AGGREGATE' AND unit = 'GALLONS')
        OR (category = 'BASKET' AND unit = 'COP')
    )
);

CREATE UNIQUE INDEX uq_sales_targets_annual
    ON sales_targets (target_year, category) WHERE target_month IS NULL;

CREATE UNIQUE INDEX uq_sales_targets_monthly
    ON sales_targets (target_year, target_month, category) WHERE target_month IS NOT NULL;
