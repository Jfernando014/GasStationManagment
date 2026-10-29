import { FuelType } from './tank.dto';

export type AutonomyStatus = 'CALCULATED' | 'INCOMPLETE_MEASUREMENT' | 'NO_SALES_HISTORY';
export type AutonomyLevel = 'OK' | 'WARNING' | 'CRITICAL' | 'NOT_EVALUATED';

/** Measured level of a tank, as used by the autonomy calculation. */
export interface TankStockResponse {
  tankId: number;
  tankCode: string;
  heightCm: number | null;
  gallons: number | null;
}

export interface FuelAutonomyResponse {
  fuelType: FuelType;
  status: AutonomyStatus;
  stockGallons: number;
  expectedDailySalesGallons: number;
  /** Null when the autonomy cannot be calculated. */
  autonomyDays: number | null;
  level: AutonomyLevel;
  warningDays: number;
  criticalDays: number;
  tanks: TankStockResponse[];
}
