-- Fondo de ahorro y movimientos de caja (C-06)

CREATE TABLE IF NOT EXISTS savings_fund_movement (
  id            BIGSERIAL PRIMARY KEY,
  movement_type VARCHAR(20) NOT NULL,
  movement_date DATE NOT NULL,
  amount        NUMERIC(14, 2) NOT NULL,
  description   VARCHAR(255),
  created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT ck_savings_fund_movement_type
    CHECK (movement_type IN ('CONTRIBUTION', 'PAYMENT')),
  CONSTRAINT ck_savings_fund_movement_amount CHECK (amount > 0)
);

CREATE INDEX IF NOT EXISTS idx_savings_fund_movement_date
  ON savings_fund_movement (movement_date);

CREATE UNIQUE INDEX IF NOT EXISTS uq_savings_fund_one_contribution_per_day
  ON savings_fund_movement (movement_date)
  WHERE movement_type = 'CONTRIBUTION';

CREATE TABLE IF NOT EXISTS cash_movement (
  id            BIGSERIAL PRIMARY KEY,
  movement_type VARCHAR(20) NOT NULL,
  movement_date DATE NOT NULL,
  amount        NUMERIC(14, 2) NOT NULL,
  description   VARCHAR(255),
  created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT ck_cash_movement_type
    CHECK (movement_type IN ('INITIAL_BALANCE', 'DEPOSIT', 'ADJUSTMENT')),
  CONSTRAINT ck_cash_movement_amount CHECK (amount <> 0)
);

CREATE INDEX IF NOT EXISTS idx_cash_movement_date
  ON cash_movement (movement_date);
