export interface FuelReceptionResponse {
  id: number;
  tankId: number;
  receptionDate: string;
  gallons: number;
  supplier: string;
  createdAt: string;
  updatedAt: string;
}

export interface FuelReceptionRequest {
  tankId: number;
  receptionDate: string;
  gallons: number;
  supplier: string;
}

export interface FuelReceptionFilters {
  tankId?: number | null;
  from?: string | null;
  to?: string | null;
}
