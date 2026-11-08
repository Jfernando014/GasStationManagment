export enum JournalStatus {
    OPEN = 'OPEN',
    CLOSED = 'CLOSED'
}

export enum StepStatus {
    PENDING = 'PENDING',
    IN_PROGRESS = 'IN_PROGRESS',
    COMPLETED = 'COMPLETED'
}

export interface JournalStepResponse {
    id: number;
    stepNumber: number;
    name: string;
    status: StepStatus;
    completedAt: string | null;
}

export interface JournalResponse {
    id: number;
    date: string;
    consecutive: number;
    status: JournalStatus;
    steps: JournalStepResponse[];
    closedAt?: string | null;
}

export interface StepSummaryItemDto {
    stepNumber: number;
    stepName: string;
    status: string;
    hasDiscrepancy: boolean;
    summaryDetail: string;
}

export interface FuelVariationSummaryItemDto {
    fuelName: string;
    initialGallons: number;
    receivedGallons: number;
    soldGallons: number;
    theoreticalGallons: number;
    measuredGallons: number;
    variationGallons: number;
    toleranceGallons: number;
    status: string;
}

export interface AutonomyAlertSummaryItemDto {
    tankId: string;
    fuelName: string;
    autonomyDays: number;
    reachesUntilDate: string;
    severity: string;
}

export interface JournalClosingSummaryResponse {
    journalId: number;
    journalDate: string;
    consecutive: number;
    status: JournalStatus;
    closedAt: string | null;
    completedStepsCount: number;
    vendorsWithShortageCount: number;
    vendorsWithSurplusCount: number;
    newAlertsCount: number;
    steps: StepSummaryItemDto[];
    fuelVariations: FuelVariationSummaryItemDto[];
    newAutonomyAlerts: AutonomyAlertSummaryItemDto[];
}

export type HistoryDaySource = 'JOURNAL' | 'MIGRATED' | 'EMPTY';
export type HistoryDayState = 'CLOSED' | 'OPEN' | 'MIGRATED' | 'PENDING';
export type HistoryDayMarker = 'DISCREPANCY' | 'ALERT';

export interface JournalHistoryDaySummaryDto {
    date: string;
    dayOfMonth: number;
    source: HistoryDaySource;
    state: HistoryDayState;
    journalId: number | null;
    consecutive: number | null;
    markers: HistoryDayMarker[];
}

export interface JournalHistoryMonthResponse {
    yearMonth: string;
    closedSystemCount: number;
    migratedCount: number;
    withMarkersCount: number;
    days: JournalHistoryDaySummaryDto[];
}

export interface MigratedVendorSaleRowDto {
    vendorName: string;
    shift: string;
    gallons: number;
    basketsAmount: number;
    totalSalesAmount: number;
    discrepancyAmount: number;
}

export interface MigratedCredibancoSummaryDto {
    mastercardAmount: number;
    visaAmount: number;
    qrAmount: number;
    totalEdsAmount: number;
}

export interface MigratedFuelVariationRowDto {
    fuelName: string;
    soldGallons: number;
    theoreticalGallons: number;
    physicalGallons: number;
    variationGallons: number;
}

export interface MigratedCashBalanceSummaryDto {
    sheetNumber: number;
    previousCashAmount: number;
    accumulatedSavingsAmount: number;
    dominusBalanceAmount: number;
}

export interface JournalHistoryDayDetailResponse {
    date: string;
    source: HistoryDaySource;
    state: HistoryDayState;
    journalId: number | null;
    consecutive: number | null;
    closedAt: string | null;
    markers: HistoryDayMarker[];
    vendorSales: MigratedVendorSaleRowDto[];
    credibancoSummary: MigratedCredibancoSummaryDto | null;
    fuelVariations: MigratedFuelVariationRowDto[];
    cashBalanceSummary: MigratedCashBalanceSummaryDto | null;
}
