import { FuelType } from '../../../shared/models/dtos/tank.dto';

/** Where the card percentage of a month comes from. */
export type CardPercentageMode = 'REAL' | 'MANUAL';

export type MissingDatum = 'SALE_PRICE' | 'PURCHASE_COST' | 'FREIGHT_PER_GALLON' | 'FINANCIAL_COST_PER_GALLON';

/** Percentages go from 0 to 100; the commission rate is a fraction (0.0265 = 2,65 %). */
export interface CardCommissionDto {
  mode: CardPercentageMode;
  appliedPercentage: number | null;
  manualPercentage: number | null;
  realPercentage: number | null;
  cardSales: number;
  totalSales: number;
  realPercentageNote: string;
  commissionRate: number;
  commissionRateDate: string;
}

export interface FuelUnitValuesDto {
  salePrice: number | null;
  purchaseCost: number | null;
  freightPerGallon: number | null;
  financialCostPerGallon: number | null;
}

export interface FuelDayDto extends FuelUnitValuesDto {
  date: string;
  gallons: number;
  marginPerGallon: number | null;
  sales: number | null;
  profit: number | null;
}

export interface MissingDataDto {
  datum: MissingDatum;
  /** First day with gallons sold on which the figure was not in force. */
  since: string;
}

/** Null means that the figure cannot be calculated, never zero. */
export interface FuelProfitDto {
  fuelType: FuelType;
  complete: boolean;
  missingData: MissingDataDto[];
  gallons: number;
  sales: number | null;
  realProfit: number | null;
  targetGallons: number | null;
  compliancePercentage: number | null;
  targetProfit: number | null;
  referenceMarginPerGallon: number | null;
  commission: number | null;
  profitAfterCommission: number | null;
  referenceValues: FuelUnitValuesDto | null;
  days: FuelDayDto[];
}

export interface FuelProfitTotalsDto {
  complete: boolean;
  incompleteFuels: FuelType[];
  fuelsWithoutTarget: FuelType[];
  gallons: number | null;
  sales: number | null;
  realProfit: number | null;
  commission: number | null;
  profitAfterCommission: number | null;
  targetGallons: number | null;
  compliancePercentage: number | null;
  targetProfit: number | null;
}

export interface FuelProfitReportDto {
  month: string;
  referenceDate: string;
  daysInMonth: number;
  daysWithData: number;
  card: CardCommissionDto;
  fuels: FuelProfitDto[];
  totals: FuelProfitTotalsDto;
}

export interface CardSettingDto {
  mode: CardPercentageMode;
  manualPercentage: number | null;
}
