export interface TankReadingRequest {
  tankId: number;
  heightCm: number;
}

export interface TankMeasurementRequest {
  readings: TankReadingRequest[];
}

export interface TankReadingResponse {
  tankId: number;
  heightCm: number;
  volumeGallons: number;
}

export interface TankMeasurementResponse {
  id: number;
  journalDate: string;
  measuredAt: string;
  readings: TankReadingResponse[];
}

/** Gallons of a measured height, calculated by the backend with the tank gauge table. */
export interface GaugeVolumeResponse {
  tankId: number;
  heightCm: number;
  volumeGallons: number;
}
