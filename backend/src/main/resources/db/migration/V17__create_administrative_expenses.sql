CREATE TABLE administrative_expense (
    id BIGSERIAL PRIMARY KEY,
    expense_date DATE NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_administrative_expense_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_administrative_expense_date
    ON administrative_expense (expense_date DESC, id DESC);