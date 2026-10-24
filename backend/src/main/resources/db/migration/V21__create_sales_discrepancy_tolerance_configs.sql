CREATE TABLE sales_discrepancy_tolerance_configs (
    id BIGSERIAL PRIMARY KEY,
    tolerance_amount NUMERIC(14, 2) NOT NULL,
    effective_date DATE NOT NULL,
    notes VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) DEFAULT 'SYSTEM',
    CONSTRAINT chk_sales_tolerance_positive CHECK (tolerance_amount >= 0),
    CONSTRAINT uq_sales_tolerance_effective_date UNIQUE (effective_date)
);

-- Seed initial baseline tolerance configuration for August 2026 operations
INSERT INTO sales_discrepancy_tolerance_configs (tolerance_amount, effective_date, notes, created_at, created_by)
VALUES (0.00, '2026-08-01', 'Initial baseline tolerance (pending client definition)', CURRENT_TIMESTAMP, 'SYSTEM');
