import { CashDifferenceStatus } from '../models/dtos/cash-balance-step4.dto';
import { formatCop } from './format.helpers';
import { parseAmount } from './electronic-payments.helpers';

export const MAX_CASH_BALANCE_PDF_BYTES = 25 * 1024 * 1024;
export const MAX_FUND_PAYMENT_DESCRIPTION_LENGTH = 255;

export type CashBalanceTone = 'ok' | 'warn' | 'pending';

export function cashBalanceTone(status: CashDifferenceStatus | null): CashBalanceTone {
  if (status === null) return 'pending';
  return status === 'CUADRA' ? 'ok' : 'warn';
}

export function cashDifferenceLabel(status: CashDifferenceStatus | null): string {
  switch (status) {
    case 'CUADRA':
      return 'Cuadra';
    case 'PLANILLA_MAYOR':
      return 'Planilla mayor';
    case 'PLANILLA_MENOR':
      return 'Planilla menor';
    default:
      return 'Pendiente';
  }
}

export function cashDifferenceDetail(status: CashDifferenceStatus | null): string {
  switch (status) {
    case 'CUADRA':
      return 'El saldo de la planilla coincide con el de Dominus.';
    case 'PLANILLA_MAYOR':
      return 'La planilla tiene más efectivo que Dominus.';
    case 'PLANILLA_MENOR':
      return 'La planilla tiene menos efectivo que Dominus.';
    default:
      return 'Aún no se puede calcular la diferencia.';
  }
}

export interface FundPaymentValidation {
  amount: number | null;
  error: string | null;
}

export function validateFundPayment(input: {
  date: string;
  amountText: string;
  balance: number | null;
}): FundPaymentValidation {
  if (!input.date) return { amount: null, error: 'Elige la fecha del pago.' };
  const amount = parseAmount(input.amountText);
  if (amount === null) return { amount: null, error: 'Escribe un valor válido, con máximo dos decimales.' };
  if (amount <= 0) return { amount: null, error: 'Escribe un valor mayor que cero.' };
  if (input.balance !== null && amount > input.balance) {
    return { amount: null, error: `El pago supera el saldo del fondo (${formatCop(input.balance)}).` };
  }
  return { amount, error: null };
}

export function validateCashBalancePdf(file: { name: string; size: number; type: string }): string | null {
  const looksLikePdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
  if (!looksLikePdf) return 'El archivo debe ser un PDF.';
  if (file.size === 0) return 'El archivo está vacío.';
  if (file.size > MAX_CASH_BALANCE_PDF_BYTES) return 'El PDF supera el tamaño máximo de 25 MB.';
  return null;
}

export function hasPdfSignature(bytes: Uint8Array): boolean {
  const limit = Math.min(bytes.length - 4, 1024);
  for (let index = 0; index < limit; index++) {
    if (
      bytes[index] === 0x25 &&
      bytes[index + 1] === 0x50 &&
      bytes[index + 2] === 0x44 &&
      bytes[index + 3] === 0x46 &&
      bytes[index + 4] === 0x2d
    ) {
      return true;
    }
  }
  return false;
}
