-- Paso 4 de la jornada (C-06): saldo acumulado que reporta Dominus cada dia
-- (accumulatedCash.accumulatedBalance de su complemento de efectivo). Una fila por dia; se reemplaza al corregirla.
CREATE TABLE IF NOT EXISTS dominus_cash_balance (
  business_date       DATE PRIMARY KEY,
  accumulated_balance NUMERIC(14, 2) NOT NULL,
  updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
