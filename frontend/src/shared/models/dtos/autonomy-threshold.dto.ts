/** Days of autonomy that trigger a warning or a critical alert, valid from a journal date. */
export interface AutonomyThresholdResponse {
  id: number;
  /** ISO date (yyyy-MM-dd) the thresholds apply from. */
  validFrom: string;
  warningDays: number;
  criticalDays: number;
  createdAt: string;
}

export interface AutonomyThresholdRequest {
  validFrom: string;
  warningDays: number;
  criticalDays: number;
}
