import { HttpClient } from '@angular/common/http';
import { Injectable, Injector, WritableSignal, inject, signal } from '@angular/core';
import { retry } from 'rxjs';

import { compactHoursLabel, isCustomShiftCode, replaceCustomCodes } from '../../shared/helpers/support-hours.helpers';
import { segmentsLabel } from '../../shared/helpers/shift-display.helpers';
import { ShiftCode, ShiftScheduleSegment } from '../../shared/models/dtos/shift-catalog.dto';
import { getApiBaseUrl } from '../config/api-base';

/**
 * What the screens write instead of a custom support code (`AP_10-17`): its hours, read from the catalog once per
 * code. The methods read a signal, so a template or a computed that calls them updates when the hours arrive.
 * Any other code is returned as it is.
 */
@Injectable({
  providedIn: 'root'
})
export class ShiftHoursService {
  /** HttpClient is resolved on the first request: showing standard codes needs no HTTP at all. */
  private readonly injector = inject(Injector);
  private readonly cache = new Map<string, WritableSignal<ShiftScheduleSegment[] | null>>();

  /** Hours of a custom code; null while they load, empty when they could not be read. */
  segments(code: string): ShiftScheduleSegment[] | null {
    let entry = this.cache.get(code);
    if (!entry) {
      entry = signal<ShiftScheduleSegment[] | null>(null);
      this.cache.set(code, entry);
      this.load(code, entry);
    }
    return entry();
  }

  /** Short text of a code: "10–17" for a custom one. `known` avoids the request when the caller has the hours. */
  text(code: string, known?: ShiftScheduleSegment[] | null): string {
    if (!isCustomShiftCode(code)) return code;
    const segments = known?.length ? known : this.segments(code);
    if (segments === null) return '…';
    return segments.length ? compactHoursLabel(segments) : 'Apoyo';
  }

  /** Full hours of a custom code: "10:00–17:00". */
  fullText(code: string, known?: ShiftScheduleSegment[] | null): string {
    const segments = known?.length ? known : this.segments(code);
    return segments?.length ? segmentsLabel(segments) : this.text(code, known);
  }

  /** A message of the server with every custom code inside it replaced by its hours. */
  inText(message: string): string {
    return replaceCustomCodes(message, code => this.text(code));
  }

  private load(code: string, entry: WritableSignal<ShiftScheduleSegment[] | null>): void {
    // Deferred: the first read can happen inside a computed or a template, where nothing may be written
    queueMicrotask(() => {
      this.injector
        .get(HttpClient)
        .get<ShiftCode>(`${getApiBaseUrl()}/api/shifts/catalog/${encodeURIComponent(code)}`)
        .pipe(retry({ count: 1, delay: 800 }))
        .subscribe({
          next: shift => entry.set(shift.schedule.segments),
          error: () => entry.set([])
        });
    });
  }
}
