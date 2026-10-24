import { AbstractControl, ValidationErrors } from '@angular/forms';
import { BasketGroup, BasketSeller, BasketStatisticsResponse } from '../models/dtos/basket-statistics.dto';
import { Worker } from '../models/dtos/worker.dto';
import { Competitor, FuelComparison, PriceComparison, PriceDifference, PriceObservation, PriceSimulation, PriceSummary } from '../models/dtos/competition.dto';
import { addExact, decimal, difference, divideExact, exactJson, participation, signedDecimal } from './sales-statistics.helpers';
import { decimalText } from './sales-target.helpers';

type Row = Record<string, unknown>;
function row(value: unknown): Row {
  if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error('Respuesta del servidor inválida.');
  return value as Row;
}
const optionalDecimal = (value: unknown): string | null => value === null ? null : decimal(value);
const count = (value: unknown): number => {
  const result = Number(value); if (!Number.isSafeInteger(result) || result < 0) throw new Error('Conteo inválido'); return result;
};
const optionalCount = (value: unknown): number | null => value === null ? null : count(value);
function summary(value: unknown): PriceSummary { const s = row(value); return { competitorCount: count(s['competitorCount']), average: optionalDecimal(s['average']), minimum: optionalDecimal(s['minimum']) }; }
function delta(value: unknown): PriceDifference { const s = row(value); return { absoluteCopPerGallon: signedDecimal(s['absoluteCopPerGallon']), percentage: signedDecimal(s['percentage']) }; }
function observation(value: unknown): PriceObservation { const s = row(value); return { ...s, pricePerGallon: decimal(s['pricePerGallon']) } as unknown as PriceObservation; }
export function parseCompetitors(text: string): Competitor[] {
  const data: unknown = exactJson(text); if (!Array.isArray(data)) throw new Error('Lista de estaciones inválida');
  return data.map(value => parseCompetitorRow(value));
}
function parseCompetitorRow(value: unknown): Competitor {
  const s = row(value);
  if (typeof s['id'] !== 'string' || !/^[1-9]\d*$/.test(s['id']) || typeof s['name'] !== 'string' || typeof s['brand'] !== 'string' || typeof s['location'] !== 'string' || typeof s['active'] !== 'boolean') throw new Error('Estación inválida');
  return s as unknown as Competitor;
}
export const parseCompetitor = (text: string): Competitor => parseCompetitorRow(exactJson(text));
export const parsePriceObservation = (text: string): PriceObservation => observation(exactJson(text));
export function parseComparison(text: string): PriceComparison {
  const data = row(exactJson(text));
  if (data['unit'] !== 'COP_PER_GALLON' || !Array.isArray(data['fuels'])) throw new Error('Comparación de precios inválida');
  return { ...data, fuels: data['fuels'].map(value => { const f = row(value); return { ...f, bellavistaPrice: optionalDecimal(f['bellavistaPrice']),
    competition: summary(f['competition']), versusAverage: delta(f['versusAverage']), versusMinimum: delta(f['versusMinimum']),
    observations: (f['observations'] as unknown[]).map(observation) } as unknown as FuelComparison; }) } as unknown as PriceComparison;
}
export function parseSimulation(text: string): PriceSimulation {
  const data = row(exactJson(text)), margin = row(data['estimatedMargin']);
  if (data['unit'] !== 'COP_PER_GALLON') throw new Error('Simulación inválida');
  return { ...data, proposedPrice: decimal(data['proposedPrice']), acquisitionCost: optionalDecimal(data['acquisitionCost']),
    competition: summary(data['competition']), versusAverage: delta(data['versusAverage']), versusMinimum: delta(data['versusMinimum']),
    estimatedMargin: { grossCopPerGallon: signedDecimal(margin['grossCopPerGallon']), grossMarginPercentage: signedDecimal(margin['grossMarginPercentage']) },
    observations: (data['observations'] as unknown[]).map(observation) } as unknown as PriceSimulation;
}
export function parseBasketStatistics(text: string): BasketStatisticsResponse {
  const data = row(exactJson(text)), charts = row(data['charts']);
  if (charts['source'] !== 'REAL_HISTORICAL_SALES' || !['AVAILABLE', 'NO_DATA', 'UNAVAILABLE'].includes(String(charts['availability'])) ||
      data['moneyUnit'] !== 'COP' || data['quantityUnit'] !== 'PRODUCT_UNITS' || !Array.isArray(data['groups'])) throw new Error('Estadísticas de canastilla inválidas');
  const available = charts['availability'] === 'AVAILABLE';
  if (!available && (data['summary'] !== null || data['groups'].length > 0)) throw new Error('Disponibilidad de canastilla inconsistente');
  const s = data['summary'] === null ? null : row(data['summary']);
  return { ...data, summary: s ? { amount: decimal(s['amount']), quantity: decimal(s['quantity']), saleLines: count(s['saleLines']), workedShifts: optionalCount(s['workedShifts']), averageAmountPerWorkedShift: optionalDecimal(s['averageAmountPerWorkedShift']) } : null,
    groups: data['groups'].map(value => { const g = row(value); return { ...g, amount: decimal(g['amount']), quantity: decimal(g['quantity']),
      saleLines: count(g['saleLines']), workedShifts: optionalCount(g['workedShifts']), participationPercent: optionalDecimal(g['participationPercent']), averageAmountPerWorkedShift: optionalDecimal(g['averageAmountPerWorkedShift']) } as unknown as BasketGroup; }),
    charts: { ...charts, outputScale: count(charts['outputScale']), series: (charts['series'] as unknown[]).map(value => { const s = row(value); return { ...s,
      points: (s['points'] as unknown[]).map(value => { const p = row(value); return { ...p, value: decimal(p['value']), observations: count(p['observations']), change: signedDecimal(p['change']), changePercent: signedDecimal(p['changePercent']) }; }) }; }) }
  } as unknown as BasketStatisticsResponse;
}

export function basketSellers(data: BasketStatisticsResponse, workers: Worker[] = []): BasketSeller[] {
  if (data.charts.availability !== 'AVAILABLE') return [];
  const people = new Map(workers.map(worker => [worker.id, worker]));
  const groups = new Map<string, BasketGroup[]>();
  data.groups.forEach(group => groups.set(group.workerId, [...(groups.get(group.workerId) ?? []), group]));
  return [...groups].map(([id, selected]) => {
    const amount = selected.reduce((total, group) => addExact(total, group.amount), '0');
    const shifts = selected[0].workedShifts;
    const verified = data.workedShiftsAvailability === 'AVAILABLE' && selected.every(g => g.workedShifts === shifts) ? shifts : null;
    return { workerId: id, name: people.get(id)?.fullName ?? selected[0].workerName ?? id, document: people.get(id)?.document ?? null,
      amount, quantity: selected.reduce((total, group) => addExact(total, group.quantity), '0'), workedShifts: verified,
      average: verified === null ? null : divideExact(amount, String(verified)), participation: data.summary ? participation(amount, data.summary.amount) : null, groups: selected };
  }).sort((a, b) => difference(b.amount, a.amount).startsWith('-') ? -1 : difference(b.amount, a.amount) === '0' ? a.name.localeCompare(b.name) : 1);
}

export function priceValidator(allowZero = false): (control: AbstractControl) => ValidationErrors | null {
  return control => {
    if (control.value === '' || control.value === null) return null;
    const value = typeof control.value === 'string' ? decimalText(control.value) : null;
    return value !== null && value.split('.')[0].length <= 10 && (value.split('.')[1]?.length ?? 0) <= 2 && (allowZero || value !== '0') ? null : { price: 'Usa un precio positivo menor que 10¹⁰ COP, con hasta dos decimales.' };
  };
}
