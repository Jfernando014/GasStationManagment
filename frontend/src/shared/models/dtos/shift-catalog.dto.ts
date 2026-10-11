/** Worker role as returned by the backend (RoleResponse). */
export interface RoleOption {
  id: string;
  /** TITULAR or APOYO. */
  name: string;
  dispenser: number;
}

/** Shift code of the catalog (ShiftCodeResponse), only with the fields the frontend reads. */
export interface ShiftCode {
  id: string;
  code: string;
  name: string;
  /** Null for DESCANSO. */
  role: RoleOption | null;
  active: boolean;
}
