import { FuelType } from './tank.dto';
import { AutonomyLevel, InventoryAlertResponse } from './inventory-alert.dto';

/** Stock of a fuel according to the last measurement of its tanks. */
export interface FuelStockSummaryResponse {
  fuelType: FuelType;
  currentGallons: number;
  capacityGallons: number;
  /** Current over capacity, one decimal. Null if the capacity is unknown. */
  percentage: number | null;
  /** False if some tank of the fuel has never been measured. */
  complete: boolean;
  alertLevel: AutonomyLevel | null;
}

/** A tank with its last measurement; the measurement fields are null if it was never measured. */
export interface TankSnapshotResponse {
  tankId: number;
  tankCode: string;
  fuelType: FuelType;
  heightCm: number | null;
  gallons: number | null;
  capacityGallons: number;
  percentage: number | null;
  measuredOn: string | null;
  measuredAt: string | null;
  alertLevel: AutonomyLevel | null;
}

export interface InventoryHomeResponse {
  fuels: FuelStockSummaryResponse[];
  tanks: TankSnapshotResponse[];
  /** Pending alert for the banner: critical first, then the one with less autonomy. */
  mostCriticalAlert: InventoryAlertResponse | null;
  pendingAlerts: number;
}
