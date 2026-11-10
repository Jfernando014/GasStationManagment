/** Backend contract of /api/v1/incentives/additive-sales (AdditiveSalesDtos). Only what the screen reads. */

export type SyncStatusDto = 'PROCESSED' | 'PENDING';

export interface SyncDto {
  /** Operational day, "YYYY-MM-DD". */
  date: string;
  status: SyncStatusDto;
  attempts: number;
  lastError: string | null;
}
