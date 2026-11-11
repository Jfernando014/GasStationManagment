export interface SavingsFundPaymentItem {
  id: string;
  fecha: string;
  valor: number;
  concepto: string;
}

export interface SavingsFundState {
  ahorroDiario: number;
  dias: number;
  acumulado: number;
  saldoAnterior: number;
  ahorroHoy: number;
  pagosHoy: SavingsFundPaymentItem[];
  totalPagosHoy: number;
  diasLista: number[];
}

export interface DailyExpenseItem {
  id: number;
  date: string;
  razonSocial: string;
  concepto: string;
  valor: number;
}

export interface Step4CapturedData {
  consolidadoDominus?: number | null;
  saldoDominus?: number | null;
  saldoAnterior?: number | null;
  pdf?: {
    nombre: string;
    tamano?: number;
    cargadoEn?: string;
    finalGeneradoEn?: string;
    paginas?: number;
  } | null;
}

export type ConsolidadoCuadreState = 'cuadra' | 'difiere' | 'pendiente';

export interface Step4CalculationResult {
  planilla: number;
  dinero: number;
  origenDinero: string;
  fondo: SavingsFundState;
  gastos: DailyExpenseItem[];
  recibos: number;
  semilla: boolean;
  saldoAnterior: number | null;
  origenAnterior: 'planilla histórica' | 'saldo del día anterior' | 'digitar';
  consignaciones: number;
  saldoEfectivo: number | null;
  saldoJustificado: number | null;
  saldoDominus: number | null;
  saldoDominusPegado: boolean;
  diferencia: number | null;
  totalEnCaja: number | null;
  totalEstacion: number | null;
  consolidado: number | null;
  cuadre: ConsolidadoCuadreState;
}

export interface BackendCashBalanceTableDto {
  date: string;
  journalConsecutive: number;
  journalStatus: string;
  savingsFund: {
    previousBalance: number;
    contribution: number;
    payments: number;
    accumulatedBalance: number;
  };
  expenses: {
    items: { date: string; description: string; amount: number }[];
    total: number;
  };
  cashBalance: number;
  dailyCash: number | null;
  dominusAccumulatedBalance: number | null;
  difference: number | null;
}

export interface BackendCashBalanceStepResponseDto {
  table: BackendCashBalanceTableDto;
  canComplete: boolean;
  missing: string[];
  differenceStatus: 'CUADRA' | 'PLANILLA_MAYOR' | 'PLANILLA_MENOR' | null;
}

