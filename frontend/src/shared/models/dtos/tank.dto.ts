export type FuelType = 'MOTOR' | 'DIESEL' | 'EXTRA' | 'MAX_PRO';

export interface TankResponse {
  id: number;
  code: string;
  name: string;
  fuelType: FuelType;
  maxCapacityGallons: number;
  maxHeightCm: number;
  toleranceCm: number;
  gaugeTableId?: number;
}

export interface TankRequest {
  code: string;
  name: string;
  fuelType: FuelType;
  maxCapacityGallons: number;
  maxHeightCm: number;
  toleranceCm: number;
  gaugeTableId?: number;
}
