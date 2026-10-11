/** Decimal JSON numbers are kept as text locally, without a binary float conversion. */
export type SalesTargetCategory = 'FUEL_AGGREGATE' | 'BASKET';
export type SalesTargetUnit = 'GALLONS' | 'COP';

export interface CreateSalesTargetRequest {
  year: number;
  month?: number | null;
  category: SalesTargetCategory;
  value: string;
  unit: SalesTargetUnit;
  maximumSale: string;
}

export interface SalesTargetResponse {
  id: string;
  year: number;
  month: number | null;
  category: SalesTargetCategory;
  value: string;
  unit: SalesTargetUnit;
  maximumSale: string | null;
  createdAt: string;
  message?: string;
}
