/** Mirrors origin/feature/back/period-statistics-scrum83 (44d756d). Money stays decimal text. */
export type FuelType = 'MOTOR' | 'DIESEL' | 'EXTRA' | 'MAX_PRO';
export const FUELS: { value: FuelType; label: string; short: string }[] = [
  { value: 'MOTOR', label: 'Gasolina Corriente', short: 'CORRIENTE' }, { value: 'DIESEL', label: 'Diesel (ACPM)', short: 'DIESEL (ACPM)' },
  { value: 'EXTRA', label: 'Gasolina Extra', short: 'EXTRA' }, { value: 'MAX_PRO', label: 'Max Pro Diesel', short: 'MAX PRO DIESEL' }
];
export interface Competitor { id: string; name: string; brand: string; location: string; active: boolean }
export interface CompetitorRequest { name: string; brand: string; location: string }
export interface PriceObservation { id: string; competitorId: string; fuelType: FuelType; pricePerGallon: string; observedAt: string; createdAt: string }
export interface PriceObservationRequest { competitorId: string; fuelType: FuelType; pricePerGallon: string; observedAt: string }
export interface PriceSummary { competitorCount: number; average: string | null; minimum: string | null }
export interface PriceDifference { absoluteCopPerGallon: string | null; percentage: string | null }
export interface FuelComparison {
  fuelType: FuelType; bellavistaPrice: string | null; bellavistaValidFrom: string | null;
  competition: PriceSummary; versusAverage: PriceDifference; versusMinimum: PriceDifference; observations: PriceObservation[];
}
export interface PriceComparison {
  asOf: string; bellavistaDate: string; mode: 'CURRENT_LATEST' | 'AS_OF'; unit: 'COP_PER_GALLON';
  activeCompetitorsOnly: boolean; fuels: FuelComparison[];
}
export interface PriceSimulationRequest { fuelType: FuelType; proposedPrice: string; acquisitionCost: string | null; asOf?: string }
export interface PriceSimulation {
  asOf: string; fuelType: FuelType; unit: 'COP_PER_GALLON'; proposedPrice: string; acquisitionCost: string | null;
  competition: PriceSummary; versusAverage: PriceDifference; versusMinimum: PriceDifference;
  estimatedMargin: { grossCopPerGallon: string | null; grossMarginPercentage: string | null }; observations: PriceObservation[];
}
