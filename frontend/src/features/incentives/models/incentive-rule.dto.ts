/** Backend contract of /api/v1/incentives/rules (IncentiveRuleDtos). */

export interface AdditiveIncentiveDto {
  productId: number;
  productName: string;
  valuePerUnit: number | null;
  validFrom: string | null;
  nextValuePerUnit: number | null;
  nextValidFrom: string | null;
  noIncentive: boolean;
  label: string | null;
}

export type RuleStatusDto = 'PROGRAMADO' | 'VIGENTE' | 'ANTERIOR';

export interface RuleHistoryDto {
  id: number;
  productId: number;
  valuePerUnit: number;
  validFrom: string;
  status: RuleStatusDto;
  reason: string | null;
  createdAt: string;
}

export interface CreateRuleRequestDto {
  productId: number;
  valuePerUnit: number;
  validFrom: string;
  reason: string | null;
}

export interface CreateRuleResponseDto {
  created: boolean;
  message: string;
  rule: { id: number; productId: number; valuePerUnit: number; validFrom: string; active: boolean;
    reason: string | null; createdAt: string } | null;
  warnings: { code: 'UNUSUALLY_HIGH_VALUE' | 'VALID_FROM_NOT_FIRST_DAY'; message: string }[];
}
