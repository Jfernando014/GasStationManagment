export type MonthlyQrSource = 'REDEBAN' | 'PLANILLA' | 'NONE';

export interface MonthlyReconciliationDay {
  date: string;
  hasData: boolean;
  mastercard: number;
  visa: number;
  amex: number;
  totalEds: number;
  qr: number;
  qrSource: MonthlyQrSource;
  totalWithQr: number;
  points: number;
}

export interface MonthlyReconciliationTotals {
  mastercard: number;
  visa: number;
  amex: number;
  totalEds: number;
  qr: number;
  totalWithQr: number;
  points: number;
}

export interface MonthlyCommission {
  rate: number;
  rateDate: string;
  base: number;
  amount: number;
}

export interface MonthlyReconciliation {
  month: string;
  from: string;
  to: string;
  days: MonthlyReconciliationDay[];
  daysWithData: number;
  totals: MonthlyReconciliationTotals;
  commission: MonthlyCommission;
}
