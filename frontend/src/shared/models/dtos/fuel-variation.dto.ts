import { FuelType } from './tank.dto';

export type VariationStatus =
  | 'OK'
  | 'NO_PREVIOUS_MEASUREMENT'
  | 'INCOMPLETE_MEASUREMENT'
  | 'NO_SALES_DATA';

export interface FuelVariationResponse {
  fuelType: FuelType;
  status: VariationStatus;
  tankIds: number[];
  initialGallons: number | null;
  entriesGallons: number;
  soldGallons: number | null;
  theoreticalGallons: number | null;
  measuredGallons: number | null;
  varianceGallons: number | null;
  toleranceGallons: number | null;
  outOfTolerance: boolean;
}
