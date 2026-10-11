CREATE TABLE IF NOT EXISTS administrative_expense (
    id BIGSERIAL PRIMARY KEY,
    expense_date DATE NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO administrative_expense (expense_date, amount, description, created_at)
VALUES
    ('2026-08-01', 80000.00, 'Servicios de energía', CURRENT_TIMESTAMP),
    ('2026-08-10', 25000.00, 'Limpieza y mantenimiento', CURRENT_TIMESTAMP),
    ('2026-08-14', 31100.00, 'Papelería y suministros', CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
