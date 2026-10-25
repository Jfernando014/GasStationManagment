export interface AssignedVendorDto {
  document: string;
  name: string;
  role: string;
  shiftName: string;
  pumpNumber?: number;
  workerCode?: string;
}

export interface VendorCloseProductDto {
  id?: string;
  sku: string;
  productName: string;
  category?: string;
  unitsSold: number;
  unitPrice?: number;
  totalAmount?: number;
}

export interface VendorCloseSummaryDto {
  date: string;
  vendorDocument: string;
  vendorName: string;
  shiftName: string;
  pumpNumber?: number;
  cash: number;
  ga: number;
  credibanco: number;
  qr: number;
  redemptions: number;
  totalDelivered: number;
  gallonsMotor: number;
  gallonsDiesel: number;
  gallonsExtra: number;
  gallonsMaxpro: number;
  basketDominusAmount: number;
  dominusTirilla: number;
  realDiscrepancy: number;
  resultStatus: 'CUADRA' | 'SOBRA' | 'FALTA' | string;
  products?: VendorCloseProductDto[];
}

export interface DaySummaryDto {
  date: string;
  totalCash: number;
  totalGa: number;
  totalCredibanco: number;
  totalQr: number;
  totalRedemptions: number;
  totalDelivered: number;
  totalDominusTirilla: number;
  totalDiscrepancy: number;
  gallonsMotor: number;
  gallonsDiesel: number;
  gallonsExtra: number;
  gallonsMaxpro: number;
  totalFuelGallons: number;
  basketDominusAmount: number;
  totalProductUnits: number;
  products: VendorCloseProductDto[];
}

export interface VendorShiftCloseItemRequestDto {
  id?: string;
  vendorDocument: string;
  vendorName: string;
  shiftName: string;
  pumpNumber?: number;
  cash: number;
  ga: number;
  credibanco: number;
  qr: number;
  redemptions: number;
  totalDelivered: number;
  gallonsMotor: number;
  gallonsDiesel: number;
  gallonsExtra: number;
  gallonsMaxpro: number;
  basketDominusAmount: number;
  dominusTirilla: number;
  products?: VendorCloseProductDto[];
}

export interface VendorShiftCloseBatchRequestDto {
  journalDate: string;
  closes: VendorShiftCloseItemRequestDto[];
}

export interface EffectiveDiscrepancyToleranceDto {
  id?: string;
  effectiveDate: string;
  maxDiscrepancyAmount: number;
  active?: boolean;
}

export interface DenominationsBreakdown {
  b100k: number;
  b50k: number;
  b20k: number;
  b10k: number;
  b5k: number;
  b2k: number;
}

export interface CatalogProductItem {
  sku: string;
  name: string;
  category: string;
  price: number;
}

export interface VendorGridCapture {
  vendor: AssignedVendorDto;
  parciales: number;
  pico: number;
  efectivo: number;
  ga: number;
  credibanco: number;
  qr: number;
  redenciones: number;
  totalEntregado: number;
  galMotor: number;
  galDiesel: number;
  galExtra: number;
  galMaxpro: number;
  canastilla: number;
  tirilla: number;
  descuadre: number;
  estadoResultado: 'cuadra' | 'sobra' | 'falta' | 'vacio';
  denominaciones: DenominationsBreakdown;
  totalArqueo: number;
  avisoArqueo: boolean;
  avisoTirilla: string | null;
  productos: Record<string, number>;
  conDatos: boolean;
}
