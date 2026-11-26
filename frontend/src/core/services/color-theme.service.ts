import { DOCUMENT } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, forkJoin, of, tap } from 'rxjs';

import {
  DEFAULT_COLOR_THEME,
  DEFAULT_FUEL_COLORS,
  DEFAULT_SHIFT_COLORS,
  deriveSoftSurface,
  normalizeHexColor,
  readableTextOn
} from '../../shared/helpers/color-contrast.helpers';
import {
  ColorCategory,
  ColorThemeDto,
  ColorUpdateItemRequestDto,
  FuelColorKey,
  ShiftColorKey,
  UpdateColorThemeRequestDto
} from '../../shared/models/dtos/color-settings.dto';
import { Worker } from '../../shared/models/dtos/worker.dto';
import { getApiBaseUrl } from '../config/api-base';

const STORAGE_KEY = 'eds.appearance.color-theme.v1';

export interface WorkerColorLookup {
  workerId?: string | null;
  document?: string | null;
  name?: string | null;
}

export interface ResolvedWorkerColor {
  background: string;
  text: '#FFFFFF' | '#0B192C';
  subtle: string;
  strong: string;
}

function normalizeNameKey(name: string | null | undefined): string {
  return (name ?? '').trim().toLocaleLowerCase('es-CO').replace(/\s+/g, ' ');
}

function normalizeDocumentKey(document: string | null | undefined): string {
  return (document ?? '').trim();
}

function readCachedTheme(): ColorThemeDto | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<ColorThemeDto>;
    if (!Array.isArray(parsed.fuels) || !Array.isArray(parsed.shifts) || typeof parsed.workers !== 'object') {
      return null;
    }
    return {
      fuels: parsed.fuels,
      shifts: parsed.shifts,
      workers: parsed.workers ?? {},
      palette: Array.isArray(parsed.palette) && parsed.palette.length
        ? parsed.palette
        : DEFAULT_COLOR_THEME.palette
    };
  } catch {
    return null;
  }
}

/**
 * Global reactive color theme manager (SR-09).
 * Loads editable color overrides for fuels, shift families, and individual sellers from `/api/v1/color-settings`,
 * injects CSS custom properties into `:root`, and resolves seller colors across views.
 */
@Injectable({ providedIn: 'root' })
export class ColorThemeService {
  private readonly http = inject(HttpClient, { optional: true });
  private readonly document = inject(DOCUMENT, { optional: true });
  private readonly apiUrl = `${getApiBaseUrl()}/api/v1/color-settings`;
  private readonly workersApiUrl = `${getApiBaseUrl()}/api/workers`;

  private readonly workerIdByDocument = signal<ReadonlyMap<string, string>>(new Map());
  private readonly workerIdByName = signal<ReadonlyMap<string, string>>(new Map());

  readonly theme = signal<ColorThemeDto>(DEFAULT_COLOR_THEME);
  readonly loaded = signal(false);

  readonly fuelColors = computed<Record<FuelColorKey, string>>(() => {
    const map: Record<FuelColorKey, string> = { ...DEFAULT_FUEL_COLORS };
    for (const entry of this.theme().fuels) {
      if (entry.key in map) {
        map[entry.key as FuelColorKey] = entry.colorHex;
      }
    }
    return map;
  });

  readonly shiftColors = computed<Record<ShiftColorKey, string>>(() => {
    const map: Record<ShiftColorKey, string> = { ...DEFAULT_SHIFT_COLORS };
    for (const entry of this.theme().shifts) {
      if (entry.key in map) {
        map[entry.key as ShiftColorKey] = entry.colorHex;
      }
    }
    return map;
  });

  readonly workerColors = computed<Record<string, string>>(() => this.theme().workers);

  constructor() {
    const cached = readCachedTheme();
    if (cached) {
      this.theme.set(cached);
      this.applyToDocument(cached);
    }
  }

  /** Fetches the active color configuration and worker directory index (best-effort; never blocks navigation). */
  load(): void {
    if (!this.http) return;
    forkJoin({
      theme: this.http.get<ColorThemeDto>(this.apiUrl).pipe(catchError(() => of(null))),
      workers: this.http.get<Worker[]>(this.workersApiUrl).pipe(catchError(() => of(null)))
    }).subscribe(({ theme, workers }) => {
      if (workers) {
        this.registerWorkers(workers);
      }
      if (theme) {
        this.commitTheme(theme);
        this.loaded.set(true);
      }
    });
  }

  /** Direct GET observable for the configuration UI. */
  fetchTheme(): Observable<ColorThemeDto> {
    if (!this.http) return of(this.theme());
    return this.http.get<ColorThemeDto>(this.apiUrl).pipe(
      tap(theme => {
        this.commitTheme(theme);
        this.loaded.set(true);
      })
    );
  }

  /** Persists a batch of edits (fuels, shifts, and/or workers) and updates the live theme immediately. */
  saveBatch(items: ColorUpdateItemRequestDto[]): Observable<ColorThemeDto> {
    if (!this.http) return of(this.theme());
    const payload: UpdateColorThemeRequestDto = { items };
    return this.http.put<ColorThemeDto>(this.apiUrl, payload).pipe(
      tap(theme => this.commitTheme(theme))
    );
  }

  /** Updates one key immediately. */
  updateSingle(category: ColorCategory, entityKey: string, colorHex: string): Observable<ColorThemeDto> {
    if (!this.http) return of(this.theme());
    return this.http
      .put<ColorThemeDto>(`${this.apiUrl}/${encodeURIComponent(category)}/${encodeURIComponent(entityKey)}`, { colorHex })
      .pipe(tap(theme => this.commitTheme(theme)));
  }

  /** Resets one entity key back to its default color. */
  resetSingle(category: ColorCategory, entityKey: string): Observable<ColorThemeDto> {
    if (!this.http) return of(this.theme());
    return this.http
      .delete<ColorThemeDto>(`${this.apiUrl}/${encodeURIComponent(category)}/${encodeURIComponent(entityKey)}`)
      .pipe(tap(theme => this.commitTheme(theme)));
  }

  /** Resets all overrides or a single category (`FUEL`, `SHIFT`, or `WORKER`). */
  resetAll(category?: ColorCategory): Observable<ColorThemeDto> {
    if (!this.http) return of(DEFAULT_COLOR_THEME);
    let params = new HttpParams();
    if (category) {
      params = params.set('category', category);
    }
    return this.http.delete<ColorThemeDto>(this.apiUrl, { params }).pipe(
      tap(theme => this.commitTheme(theme))
    );
  }

  /** Indexes worker documents and names so views without a workerId still resolve each seller's custom color. */
  registerWorkers(workers: Pick<Worker, 'id' | 'document' | 'fullName'>[]): void {
    const byDocument = new Map<string, string>();
    const byName = new Map<string, string>();
    for (const worker of workers) {
      const id = worker.id.toLowerCase();
      const doc = normalizeDocumentKey(worker.document);
      if (doc) byDocument.set(doc, id);
      const name = normalizeNameKey(worker.fullName);
      if (name) byName.set(name, id);
    }
    this.workerIdByDocument.set(byDocument);
    this.workerIdByName.set(byName);
  }

  /** Resolves the custom color for a seller by id, document, or full name; returns null if not customized. */
  resolveWorkerColor(lookup: WorkerColorLookup): ResolvedWorkerColor | null {
    const workers = this.workerColors();
    const directId = lookup.workerId?.trim().toLowerCase();
    const fromDocument = lookup.document ? this.workerIdByDocument().get(normalizeDocumentKey(lookup.document)) : undefined;
    const fromName = lookup.name ? this.workerIdByName().get(normalizeNameKey(lookup.name)) : undefined;
    const resolvedId = directId || fromDocument || fromName;
    if (!resolvedId) return null;

    const hex = normalizeHexColor(workers[resolvedId]);
    if (!hex) return null;

    const surface = deriveSoftSurface(hex);
    return {
      background: hex,
      text: surface.onSolid,
      subtle: surface.subtle,
      strong: surface.strong
    };
  }

  fuelHex(fuel: FuelColorKey): string {
    return this.fuelColors()[fuel] ?? DEFAULT_FUEL_COLORS[fuel];
  }

  shiftHex(shift: ShiftColorKey): string {
    return this.shiftColors()[shift] ?? DEFAULT_SHIFT_COLORS[shift];
  }

  /** Clears local cache and resets CSS variables to defaults (e.g. on logout). */
  clearSessionState(): void {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // Ignore storage access errors in private browsing.
    }
    this.theme.set(DEFAULT_COLOR_THEME);
    this.loaded.set(false);
    this.applyToDocument(DEFAULT_COLOR_THEME);
  }

  private commitTheme(theme: ColorThemeDto): void {
    this.theme.set(theme);
    this.applyToDocument(theme);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(theme));
    } catch {
      // Non-critical cache failure.
    }
  }

  /** Applies fuel and shift tokens to `:root` so every CSS consumer updates instantaneously. */
  applyToDocument(theme: ColorThemeDto): void {
    const root = this.document?.documentElement;
    if (!root) return;

    const fuels = new Map(theme.fuels.map(entry => [entry.key, entry]));
    const setFuel = (cssVar: string, key: FuelColorKey) => {
      const entry = fuels.get(key);
      if (entry?.customized) {
        root.style.setProperty(cssVar, entry.colorHex);
      } else {
        root.style.removeProperty(cssVar);
      }
    };

    setFuel('--fuel-corriente', 'MOTOR');
    setFuel('--fuel-extra', 'EXTRA');
    setFuel('--fuel-diesel', 'DIESEL');
    setFuel('--fuel-maxpro', 'MAX_PRO');

    const shifts = new Map(theme.shifts.map(entry => [entry.key, entry]));
    const setSoftShift = (prefix: string, key: ShiftColorKey) => {
      const entry = shifts.get(key);
      if (entry?.customized) {
        const tokens = deriveSoftSurface(entry.colorHex);
        root.style.setProperty(prefix, entry.colorHex);
        root.style.setProperty(`${prefix}-subtle`, tokens.subtle);
        root.style.setProperty(`${prefix}-strong`, tokens.strong);
        root.style.setProperty(`${prefix}-border`, tokens.border);
      } else {
        root.style.removeProperty(prefix);
        root.style.removeProperty(`${prefix}-subtle`);
        root.style.removeProperty(`${prefix}-strong`);
        root.style.removeProperty(`${prefix}-border`);
      }
    };

    setSoftShift('--shift-dia', 'DIA');
    setSoftShift('--shift-dia6', 'DIA6');
    setSoftShift('--shift-nueve-seis', 'NUEVE_SEIS');
    setSoftShift('--shift-apoyo', 'APOYO');
    setSoftShift('--shift-descanso', 'DESCANSO');

    const noche = shifts.get('NOCHE');
    if (noche?.customized) {
      root.style.setProperty('--shift-noche', noche.colorHex);
      root.style.setProperty('--shift-noche-text', readableTextOn(noche.colorHex));
    } else {
      root.style.removeProperty('--shift-noche');
      root.style.removeProperty('--shift-noche-text');
    }
  }
}
