import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, delay, of } from 'rxjs';

import {
  RolePerformance,
  WeekdayPerformance,
  WorkerPerformance
} from '../../shared/models/dtos/worker-profile.dto';

/**
 * Mientras Estadísticas no publique su servicio, la pantalla trabaja con datos ficticios.
 * Cuando exista el real, basta con poner esta constante en false (y ajustar este archivo y su modelo si el
 * contrato cambia): ningún componente conoce la ruta.
 */
const usePlaceholderData = true;

const PLACEHOLDER_DELAY_MS = 450;

/**
 * Historical performance of a worker, from the closed journals. Calls the statistics API directly; errors arrive
 * already converted to ApiError by the error interceptor.
 */
@Injectable({
  providedIn: 'root'
})
export class WorkerPerformanceService {
  private readonly http = inject(HttpClient);
  private readonly statisticsUrl = 'http://localhost:8080/api/v1/statistics/workers';

  getPerformance(workerId: string): Observable<WorkerPerformance> {
    if (usePlaceholderData) {
      return of(placeholderPerformance(workerId)).pipe(delay(PLACEHOLDER_DELAY_MS));
    }
    return this.http.get<WorkerPerformance>(`${this.statisticsUrl}/${encodeURIComponent(workerId)}/performance`);
  }
}

const WEEKDAYS: WeekdayPerformance['weekday'][] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY'
];

const NO_SHIFTS: RolePerformance = { shifts: 0, averageGallons: null, performancePercent: null };

/**
 * Fictitious data with the shape of the proposed contract. The worker id picks one of three cases so every state
 * of the screen can be seen: full history, very few shifts (less than three per row) and no closed journal.
 */
function placeholderPerformance(workerId: string): WorkerPerformance {
  const sum = [...workerId].reduce((total, char) => total + char.charCodeAt(0), 0);
  switch (sum % 3) {
    case 0:
      return fullHistory();
    case 1:
      return fewShifts();
    default:
      return noHistory();
  }
}

function fullHistory(): WorkerPerformance {
  const holder = [
    [22, 351.4, 104],
    [26, 362.9, 108],
    [25, 358.2, 106],
    [27, 366.8, 110],
    [24, 401.5, 112],
    [21, 428.3, 109],
    [23, 301.7, 98]
  ];
  const support: RolePerformance[] = [
    NO_SHIFTS,
    NO_SHIFTS,
    { shifts: 1, averageGallons: 212.6, performancePercent: 112 },
    { shifts: 2, averageGallons: 220.1, performancePercent: 117 },
    { shifts: 4, averageGallons: 233.7, performancePercent: 96 },
    { shifts: 3, averageGallons: 241.9, performancePercent: 101 },
    NO_SHIFTS
  ];
  return {
    periodFrom: '2025-11-03',
    periodTo: '2026-08-30',
    closedShifts: { total: 168 + 10, asHolder: 168, asSupport: 10 },
    byWeekday: WEEKDAYS.map((weekday, i) => ({
      weekday,
      asHolder: { shifts: holder[i][0], averageGallons: holder[i][1], performancePercent: holder[i][2] },
      asSupport: support[i]
    })),
    maxSale: { gallons: 612.4, date: '2026-03-21', shiftCode: 'DIA' }
  };
}

function fewShifts(): WorkerPerformance {
  const counts = [2, 1, 0, 2, 1, 0, 0];
  return {
    periodFrom: '2026-07-06',
    periodTo: '2026-08-30',
    closedShifts: { total: 6, asHolder: 6, asSupport: 0 },
    byWeekday: WEEKDAYS.map((weekday, i) => ({
      weekday,
      asHolder: counts[i]
        ? { shifts: counts[i], averageGallons: 331.2 + i * 14.5, performancePercent: 96 + i * 3 }
        : NO_SHIFTS,
      asSupport: NO_SHIFTS
    })),
    maxSale: { gallons: 402.8, date: '2026-08-12', shiftCode: 'NOCHE' }
  };
}

function noHistory(): WorkerPerformance {
  return {
    periodFrom: null,
    periodTo: null,
    closedShifts: { total: 0, asHolder: 0, asSupport: 0 },
    byWeekday: WEEKDAYS.map(weekday => ({ weekday, asHolder: NO_SHIFTS, asSupport: NO_SHIFTS })),
    maxSale: null
  };
}
