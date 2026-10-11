/** Worker as returned by the backend (WorkerResponse). */
export interface Worker {
  id: string;
  fullName: string;
  document: string;
  roleId: string;
  /** TITULAR or APOYO. */
  roleName: string;
  /** Dispenser derived from the role: TITULAR = 1, APOYO = 3. */
  dispenser: number;
  active: boolean;
}

/** Body to create a worker or replace its data. The status is changed apart. */
export interface WorkerRequest {
  fullName: string;
  document: string;
  roleId: string;
}

/** Body to activate or deactivate a worker. */
export interface WorkerStatusRequest {
  active: boolean;
}
