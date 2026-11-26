-- Fuel profit: two per gallon sales parameters (zero allowed) and the card percentage setting of each month.

ALTER TABLE sales_parameter DROP CONSTRAINT ck_sales_parameter_key;
ALTER TABLE sales_parameter ADD CONSTRAINT ck_sales_parameter_key CHECK (parameter_key IN
    ('DAILY_SAVINGS', 'CREDIBANCO_COMMISSION_RATE', 'FREIGHT_PER_GALLON', 'FINANCIAL_COST_PER_GALLON'));

-- A month without a row uses the real card percentage.
CREATE TABLE fuel_profit_card_setting (
    month_start        DATE         PRIMARY KEY,
    mode               VARCHAR(10)  NOT NULL,
    manual_percentage  NUMERIC(7,4),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_fuel_profit_card_month CHECK (EXTRACT(DAY FROM month_start) = 1),
    CONSTRAINT ck_fuel_profit_card_mode CHECK (mode IN ('REAL', 'MANUAL')),
    CONSTRAINT ck_fuel_profit_card_manual CHECK ((mode = 'MANUAL') = (manual_percentage IS NOT NULL)),
    CONSTRAINT ck_fuel_profit_card_percentage CHECK (manual_percentage BETWEEN 0 AND 100)
);
