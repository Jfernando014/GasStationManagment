import { AlertLevel } from '../../../shared/models/dtos/alert-level.dto';
import { FuelType } from '../../../shared/models/dtos/tank.dto';
import { VariationAlertSettingResponse } from '../../../shared/models/dtos/variation-alert-setting.dto';

/** Whether a day could be calculated, or why not. */
export type VariationDayStatus = 'OK' | 'NO_PREVIOUS_MEASUREMENT' | 'INCOMPLETE_MEASUREMENT' | 'NO_SALES_DATA';

/** Size of the shortage or surplus of a day, in gallons. */
export type GallonRange =
  | 'FROM_1_TO_49'
  | 'FROM_50_TO_74'
  | 'FROM_75_TO_124'
  | 'FROM_125_TO_249'
  | 'FROM_250_TO_499'
  | 'FROM_500';

/** A calendar day of a fuel. Null means unknown, never zero. */
export interface DailyVariationDto {
  /** ISO date (yyyy-MM-dd). */
  date: string;
  status: VariationDayStatus;
  initialGallons: number | null;
  entriesGallons: number | null;
  soldGallons: number | null;
  theoreticalGallons: number | null;
  measuredGallons: number | null;
  /** Measured minus theoretical: positive is a surplus, negative a shortage. */
  varianceGallons: number | null;
  variancePercentage: number | null;
  toleranceGallons: number | null;
  outOfTolerance: boolean | null;
  /** Sales of the calculated days up to this one; null unless this day is calculated. */
  cumulativeSoldGallons: number | null;
  cumulativeVarianceGallons: number | null;
  cumulativePercentage: number | null;
  cumulativeStatus: AlertLevel | null;
}

export interface MonthlyVariationTotalsDto {
  /** Entries and sales of every recorded day. */
  entriesGallons: number | null;
  soldGallons: number | null;
  /** Variance of the calculated days, also as a percentage of their sales. */
  varianceGallons: number | null;
  variancePercentage: number | null;
  averageDailySoldGallons: number | null;
  lastMeasuredGallons: number | null;
  lastMeasuredDate: string | null;
}

export interface ShortageRangeDto {
  range: GallonRange;
  count: number;
  limit: number;
  /** OK below the limit, REVISAR on it, ALARMA above it; null without calculated days. */
  status: AlertLevel | null;
}

export interface SurplusRangeDto {
  range: GallonRange;
  count: number;
}

export interface FuelVariationSummaryDto {
  cumulativeStatus: AlertLevel | null;
  lastCalculatedDate: string | null;
  calculatedDays: number;
  outOfToleranceDays: number;
}

export interface FuelMonthlyVariationDto {
  fuelType: FuelType;
  /** One entry per calendar day of the month. */
  days: DailyVariationDto[];
  totals: MonthlyVariationTotalsDto;
  targetGallons: number | null;
  remainingGallons: number | null;
  shortageRanges: ShortageRangeDto[];
  surplusRanges: SurplusRangeDto[];
  summary: FuelVariationSummaryDto;
}

export interface MonthlyVariationDto {
  /** "yyyy-MM" */
  month: string;
  /** Last day of the month, or today while it is in progress. */
  referenceDate: string;
  /** Rules applied to the whole month. */
  alertSetting: VariationAlertSettingResponse;
  fuels: FuelMonthlyVariationDto[];
}
