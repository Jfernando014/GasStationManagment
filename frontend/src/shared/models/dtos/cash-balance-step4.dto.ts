export type CashDifferenceStatus = 'CUADRA' | 'PLANILLA_MAYOR' | 'PLANILLA_MENOR';

export interface SavingsFundInfoDto {
  previousBalance: number;
  contribution: number;
  payments: number;
  accumulatedBalance: number;
}

export interface ExpenseItemDto {
  date: string;
  description: string;
  amount: number;
}

export interface ExpensesInfoDto {
  items: ExpenseItemDto[];
  total: number;
}

export interface CashBalanceTableDto {
  date: string;
  journalConsecutive: number;
  journalStatus: string;
  savingsFund: SavingsFundInfoDto;
  expenses: ExpensesInfoDto;
  cashBalance: number;
  dailyCash: number | null;
  planillaCashBalance: number | null;
  dominusAccumulatedBalance: number | null;
  difference: number | null;
}

export interface CashBalanceStepResponseDto {
  table: CashBalanceTableDto;
  canComplete: boolean;
  missing: string[];
  differenceStatus: CashDifferenceStatus | null;
}

export interface SavingsFundMovementRequestDto {
  date: string;
  amount: number;
  description?: string;
}

export interface SavingsFundMovementResponseDto {
  id: number;
  type: 'CONTRIBUTION' | 'PAYMENT';
  date: string;
  amount: number;
  description: string | null;
}

export interface BalanceResponseDto {
  date: string;
  balance: number;
}
