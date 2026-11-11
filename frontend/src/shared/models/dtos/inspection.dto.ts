export type InspectionCategory = 'ONAC' | 'HERMETICIDAD' | 'AMBIENTAL' | 'BOMBAS';

export type InspectionStatus = 'VIGENTE' | 'PROXIMO' | 'VENCIDO';

export const INSPECTION_CATEGORIES: InspectionCategory[] = ['ONAC', 'HERMETICIDAD', 'AMBIENTAL', 'BOMBAS'];

export const INSPECTION_STATUSES: InspectionStatus[] = ['VIGENTE', 'PROXIMO', 'VENCIDO'];

export interface InspectionResponse {
  id: number;
  code: string;
  category: InspectionCategory;
  target: string;
  dateExecuted: string;
  dateDue: string;
  certifier: string;
  toleranceMeasured: string;
  /** Computed by the backend from the due date; it is never sent back. */
  status: InspectionStatus;
  certDoc: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface InspectionRequest {
  code: string;
  category: InspectionCategory;
  target: string;
  dateExecuted: string;
  dateDue: string;
  certifier: string;
  toleranceMeasured: string;
  certDoc: string | null;
}

export interface InspectionFilters {
  category?: InspectionCategory | null;
  status?: InspectionStatus | null;
  from?: string | null;
  to?: string | null;
}
