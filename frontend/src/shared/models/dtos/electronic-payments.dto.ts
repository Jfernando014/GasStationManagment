/** Contratos del paso 3 de la jornada (medios electrónicos). Los importes llegan como número. */

export type AssignmentStatus = 'ASSIGNED' | 'NO_TIME' | 'NO_SHIFT' | 'AMBIGUOUS';
export type PointsStatus = 'COMPLETED_ZERO' | 'PENDING_TOTAL' | 'MATCH' | 'DIFFERENCE';

/** Comprobantes de un vendedor contra el Credibanco de su planilla. difference = comprobantes - planilla. */
export interface VendorCredibancoCheck {
  workerId: string;
  workerName: string | null;
  shiftCode: string | null;
  vouchersTotal: number;
  planillaCredibanco: number;
  difference: number;
  matches: boolean;
}

export interface DayFranchiseTotals {
  amex: number;
  mastercard: number;
  visa: number;
  amexDawn: number;
  mastercardDawn: number;
  visaDawn: number;
  qr: number;
}

/** Reporte contable: el QR y el total solo se suman cuando todos los vendedores cuadran. */
export interface AccountingReport {
  amex: number;
  mastercard: number;
  visa: number;
  qr: number | null;
  total: number | null;
}

export interface PeriodTotals {
  mastercard: number;
  visa: number;
  amex: number;
  qr: number;
  totalEds: number;
  source: string;
}

export interface CredibancoDayResult {
  date: string;
  vendors: VendorCredibancoCheck[];
  allMatch: boolean;
  franchises: DayFranchiseTotals;
  qrAdded: boolean;
  totalEds: number;
  accounting: AccountingReport;
  monthToDate: PeriodTotals;
  previousMonth: PeriodTotals;
  commissionRate: number;
  commissionBase: number;
  commission: number;
}

export interface UnassignedTransaction {
  row: number;
  time: string | null;
  amount: number;
  reference: string | null;
  status: AssignmentStatus;
}

/** QR asignado a un vendedor desde Redeban contra el QR de su planilla. difference = Redeban - planilla. */
export interface VendorQrCheck {
  workerId: string;
  workerName: string | null;
  shiftCode: string | null;
  redeban: number;
  planilla: number;
  difference: number;
  matches: boolean;
}

export interface QrReconciliationResult {
  date: string;
  redebanLoaded: boolean;
  redebanTotal: number;
  planillaTotal: number;
  qrTotal: number;
  difference: number;
  matchesDay: boolean;
  unassignedTotal: number;
  perVendorReliable: boolean;
  byStatus: Partial<Record<AssignmentStatus, number>>;
  unassigned: UnassignedTransaction[];
  vendors: VendorQrCheck[];
}

export interface VendorRedemption {
  workerId: string;
  workerName: string | null;
  redemptions: number;
}

export interface PointsResult {
  date: string;
  declared: number;
  pointsTotal: number | null;
  difference: number | null;
  status: PointsStatus;
  source: string | null;
  vendors: VendorRedemption[];
}

export interface RedebanImportResponse {
  reconciliation: QrReconciliationResult;
  totalRows: number;
  accepted: number;
  ignoredNotQr: number;
  ignoredNotApproved: number;
  ignoredOtherDay: number;
}

/** Por qué todavía no se puede completar el paso 3. */
export interface StepBlocker {
  code: string;
  message: string;
}

/** Fila editable de un vendedor: comprobantes digitados y cifras de su planilla (paso 2, solo lectura). */
export interface ElectronicVendorRow {
  workerId: string;
  workerName: string | null;
  shiftCode: string | null;
  mastercard: number;
  visa: number;
  amex: number;
  mastercardDawn: number;
  visaDawn: number;
  amexDawn: number;
  planillaCredibanco: number;
  planillaQr: number;
  planillaRedemptions: number;
}

/** Respuesta de GET /api/v1/sales/electronic-payments/{date}. */
export interface ElectronicPaymentsStatus {
  date: string;
  canComplete: boolean;
  blockers: StepBlocker[];
  vendors: ElectronicVendorRow[];
  credibanco: CredibancoDayResult;
  qr: QrReconciliationResult;
  points: PointsResult;
}

/** Cuerpo de PUT .../vendors/{workerId}/vouchers: siempre los seis importes. */
export interface VoucherRequest {
  mastercard: number;
  visa: number;
  amex: number;
  mastercardDawn: number;
  visaDawn: number;
  amexDawn: number;
}

export type VoucherField = keyof VoucherRequest;
