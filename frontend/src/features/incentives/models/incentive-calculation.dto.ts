/** Backend contract of /api/v1/incentives/monthly (IncentiveCalculationDtos). Money is whole pesos. */

export type UnresolvedReasonDto = 'PRODUCT_NOT_FOUND' | 'NO_RULE_IN_FORCE';

export interface IncentiveLineDto {
  sku: string;
  productName: string;
  units: number;
  valuePerUnit: number;
  subtotal: number;
  /** First day of the month this value applied: the later of the rule start and the 1st. "YYYY-MM-DD". */
  periodStart: string;
  /** Last day it applied; null when it held until the end of the month (until today in the current month). */
  periodEnd: string | null;
}

export interface UnresolvedUnitsDto {
  sku: string;
  productName: string;
  units: number;
  reason: UnresolvedReasonDto;
  message: string;
}

export interface VendorIncentiveDto {
  vendorDocument: string;
  vendorName: string | null;
  lines: IncentiveLineDto[];
  unresolved: UnresolvedUnitsDto[];
  monthTotal: number;
  accumulatedTotal: number;
  accumulatedUnresolvedUnits: number;
}

export interface MonthlyIncentivesDto {
  /** "yyyy-MM". */
  month: string;
  /** First month of the accumulated total, "yyyy-MM". */
  accumulatedFrom: string;
  vendors: VendorIncentiveDto[];
}
