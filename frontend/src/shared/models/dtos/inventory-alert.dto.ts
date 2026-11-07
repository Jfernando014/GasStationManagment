import { FuelType } from './tank.dto';

export type AlertStatus = 'ACTIVE' | 'READ' | 'ATTENDED' | 'RESOLVED';
export type AlertType = 'AUTONOMY';
export type AutonomyLevel = 'OK' | 'WARNING' | 'CRITICAL' | 'NOT_EVALUATED';

/** Alert about the days of autonomy of a fuel. */
export interface InventoryAlertResponse {
  id: number;
  alertType: AlertType;
  fuelType: FuelType;
  level: AutonomyLevel;
  status: AlertStatus;
  autonomyDays: number;
  /** Journal date on which the condition appeared. */
  openedOn: string;
  createdAt: string;
  readAt: string | null;
  attendedAt: string | null;
  attendNote: string | null;
  resolvedAt: string | null;
}

/** Alerts nobody has attended yet (ACTIVE or READ): the number on the bell. */
export interface AlertCountResponse {
  pending: number;
}

export interface AttendAlertRequest {
  note?: string;
}
