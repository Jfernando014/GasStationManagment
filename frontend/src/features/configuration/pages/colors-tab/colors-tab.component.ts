import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { catchError, forkJoin, of } from 'rxjs';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { AvatarComponent } from '../../../../atomic-design/atoms/avatar/avatar.component';
import { SearchFieldComponent } from '../../../../atomic-design/atoms/search-field/search-field.component';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { AuthService } from '../../../../core/services/auth.service';
import { ColorThemeService } from '../../../../core/services/color-theme.service';
import { ToastService } from '../../../../core/services/toast.service';
import { WorkersService } from '../../../../core/services/workers.service';
import {
  DEFAULT_CURATED_PALETTE,
  deriveSoftSurface,
  normalizeHexColor,
  readableTextOn,
  validateEditableHex
} from '../../../../shared/helpers/color-contrast.helpers';
import { getErrorMessage } from '../../../../shared/helpers/problem-details.helpers';
import {
  ColorCategory,
  ColorThemeDto,
  ColorUpdateItemRequestDto,
  ResolvedColorEntryDto,
  ShiftColorKey
} from '../../../../shared/models/dtos/color-settings.dto';
import { Worker } from '../../../../shared/models/dtos/worker.dto';

interface ShiftPreviewStyle {
  background: string;
  color: string;
  borderColor: string;
  sampleCode: string;
}

const SHIFT_SAMPLE_CODES: Record<ShiftColorKey, string> = {
  DIA: 'DIA',
  DIA6: 'DIA6',
  NUEVE_SEIS: '9-6',
  NOCHE: 'NOCHE',
  APOYO: 'APOYO',
  DESCANSO: 'DESCANSO'
};

/**
 * Configuración › Parámetros › Colores: editable colors for fuels, shift families, and individual sellers (SR-09).
 */
@Component({
  selector: 'app-colors-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ActionButtonDirective,
    AvatarComponent,
    SkeletonComponent,
    TagComponent,
    EmptyStateComponent,
    SearchFieldComponent
  ],
  templateUrl: './colors-tab.component.html',
  styleUrl: './colors-tab.component.css'
})
export class ColorsTabComponent {
  private readonly colorTheme = inject(ColorThemeService);
  private readonly workersService = inject(WorkersService);
  private readonly toast = inject(ToastService);

  protected readonly canEdit = inject(AuthService).isAdmin();
  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly saving = signal(false);
  protected readonly resetConfirmOpen = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly workers = signal<Worker[]>([]);
  protected readonly workersLoadFailed = signal(false);
  protected readonly workerQuery = signal('');
  protected readonly activePaletteId = signal<string | null>(null);

  protected readonly fuelDrafts = signal<Record<string, string>>({});
  protected readonly shiftDrafts = signal<Record<string, string>>({});
  protected readonly workerDrafts = signal<Record<string, string>>({});

  protected readonly theme = this.colorTheme.theme;
  protected readonly palette = computed(() => {
    const serverPalette = this.theme().palette;
    return serverPalette.length ? serverPalette : DEFAULT_CURATED_PALETTE;
  });

  protected readonly filteredWorkers = computed(() => {
    const query = this.workerQuery().trim().toLocaleLowerCase('es-CO');
    const sorted = [...this.workers()].sort((a, b) => {
      if (a.active !== b.active) return a.active ? -1 : 1;
      return a.fullName.localeCompare(b.fullName, 'es-CO');
    });
    if (!query) return sorted;
    return sorted.filter(worker =>
      worker.fullName.toLocaleLowerCase('es-CO').includes(query) ||
      worker.document.toLocaleLowerCase('es-CO').includes(query) ||
      worker.roleName.toLocaleLowerCase('es-CO').includes(query)
    );
  });

  protected readonly fuelErrors = computed<Record<string, string | null>>(() => {
    const drafts = this.fuelDrafts();
    const errors: Record<string, string | null> = {};
    for (const entry of this.theme().fuels) {
      errors[entry.key] = validateEditableHex(drafts[entry.key] ?? entry.colorHex);
    }
    return errors;
  });

  protected readonly shiftErrors = computed<Record<string, string | null>>(() => {
    const drafts = this.shiftDrafts();
    const errors: Record<string, string | null> = {};
    for (const entry of this.theme().shifts) {
      errors[entry.key] = validateEditableHex(drafts[entry.key] ?? entry.colorHex);
    }
    return errors;
  });

  protected readonly workerErrors = computed<Record<string, string | null>>(() => {
    const drafts = this.workerDrafts();
    const errors: Record<string, string | null> = {};
    for (const worker of this.workers()) {
      const raw = (drafts[worker.id] ?? '').trim();
      errors[worker.id] = raw ? validateEditableHex(raw) : null;
    }
    return errors;
  });

  protected readonly hasErrors = computed(() =>
    Object.values(this.fuelErrors()).some(Boolean) ||
    Object.values(this.shiftErrors()).some(Boolean) ||
    Object.values(this.workerErrors()).some(Boolean)
  );

  protected readonly pendingChanges = computed<ColorUpdateItemRequestDto[]>(() => {
    const current = this.theme();
    const changes: ColorUpdateItemRequestDto[] = [];

    const fuelDrafts = this.fuelDrafts();
    for (const entry of current.fuels) {
      const next = normalizeHexColor(fuelDrafts[entry.key] ?? entry.colorHex);
      if (next && next !== entry.colorHex) {
        changes.push({
          category: 'FUEL',
          entityKey: entry.key,
          colorHex: next === entry.defaultHex ? null : next
        });
      }
    }

    const shiftDrafts = this.shiftDrafts();
    for (const entry of current.shifts) {
      const next = normalizeHexColor(shiftDrafts[entry.key] ?? entry.colorHex);
      if (next && next !== entry.colorHex) {
        changes.push({
          category: 'SHIFT',
          entityKey: entry.key,
          colorHex: next === entry.defaultHex ? null : next
        });
      }
    }

    const workerDrafts = this.workerDrafts();
    const serverWorkers = current.workers;
    for (const worker of this.workers()) {
      const raw = (workerDrafts[worker.id] ?? '').trim();
      const next = raw ? normalizeHexColor(raw) : null;
      const serverValue = normalizeHexColor(serverWorkers[worker.id.toLowerCase()] ?? serverWorkers[worker.id]) ?? null;
      if ((raw === '' && serverValue !== null) || (next !== null && next !== serverValue)) {
        changes.push({
          category: 'WORKER',
          entityKey: worker.id,
          colorHex: next
        });
      }
    }

    return changes;
  });

  protected readonly dirty = computed(() => this.pendingChanges().length > 0);

  protected readonly hasAnyCustomization = computed(() => {
    const current = this.theme();
    return (
      current.fuels.some(e => e.customized) ||
      current.shifts.some(e => e.customized) ||
      Object.keys(current.workers).length > 0 ||
      this.dirty()
    );
  });

  constructor() {
    inject(DestroyRef).onDestroy(() => this.toast.hide());
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.errorMessage.set(null);
    this.workersLoadFailed.set(false);

    forkJoin({
      theme: this.colorTheme.fetchTheme(),
      workers: this.workersService.list().pipe(
        catchError(() => {
          this.workersLoadFailed.set(true);
          return of<Worker[]>([]);
        })
      )
    }).subscribe({
      next: ({ theme, workers }) => {
        this.workers.set(workers);
        if (workers.length) {
          this.colorTheme.registerWorkers(workers);
        }
        this.syncDraftsFromTheme(theme, workers);
        this.state.set('ready');
      },
      error: () => {
        this.state.set('error');
      }
    });
  }

  protected fuelDraftValue(entry: ResolvedColorEntryDto): string {
    return this.fuelDrafts()[entry.key] ?? entry.colorHex;
  }

  protected shiftDraftValue(entry: ResolvedColorEntryDto): string {
    return this.shiftDrafts()[entry.key] ?? entry.colorHex;
  }

  protected workerDraftValue(workerId: string): string {
    return this.workerDrafts()[workerId] ?? '';
  }

  protected pickerHex(raw: string, fallback: string): string {
    return normalizeHexColor(raw) ?? fallback;
  }

  protected isFuelCustomized(entry: ResolvedColorEntryDto): boolean {
    const normalized = normalizeHexColor(this.fuelDraftValue(entry));
    return normalized !== null ? normalized !== entry.defaultHex : true;
  }

  protected isShiftCustomized(entry: ResolvedColorEntryDto): boolean {
    const normalized = normalizeHexColor(this.shiftDraftValue(entry));
    return normalized !== null ? normalized !== entry.defaultHex : true;
  }

  protected isWorkerCustomized(workerId: string): boolean {
    return this.workerDraftValue(workerId).trim().length > 0;
  }

  protected shiftPreview(entry: ResolvedColorEntryDto): ShiftPreviewStyle {
    const key = entry.key as ShiftColorKey;
    const hex = normalizeHexColor(this.shiftDraftValue(entry)) ?? entry.defaultHex;
    const sampleCode = SHIFT_SAMPLE_CODES[key] ?? entry.key;

    if (key === 'NOCHE') {
      return {
        background: hex,
        color: readableTextOn(hex),
        borderColor: 'transparent',
        sampleCode
      };
    }
    const surface = deriveSoftSurface(hex);
    if (key === 'NUEVE_SEIS') {
      return {
        background: '#FFFFFF',
        color: surface.strong,
        borderColor: hex,
        sampleCode
      };
    }
    if (key === 'DESCANSO') {
      return {
        background: surface.subtle,
        color: surface.strong,
        borderColor: surface.border,
        sampleCode
      };
    }
    return {
      background: surface.subtle,
      color: surface.strong,
      borderColor: 'transparent',
      sampleCode
    };
  }

  protected togglePalette(id: string): void {
    this.activePaletteId.update(current => (current === id ? null : id));
  }

  protected onColorInput(category: ColorCategory, key: string, value: string): void {
    this.errorMessage.set(null);
    this.updateDraftMap(category, key, value);
  }

  protected onPickerChange(category: ColorCategory, key: string, value: string): void {
    const normalized = normalizeHexColor(value) ?? value.toUpperCase();
    this.errorMessage.set(null);
    this.updateDraftMap(category, key, normalized);
  }

  protected pickPreset(category: ColorCategory, key: string, hex: string): void {
    this.errorMessage.set(null);
    this.updateDraftMap(category, key, hex);
  }

  protected resetSingleDraft(category: ColorCategory, key: string, defaultHex = ''): void {
    this.errorMessage.set(null);
    this.updateDraftMap(category, key, defaultHex);
  }

  protected assignDistinctActiveWorkerColors(): void {
    const swatches = this.palette();
    if (!swatches.length) return;
    const activeWorkers = this.workers()
      .filter(worker => worker.active)
      .sort((a, b) => a.fullName.localeCompare(b.fullName, 'es-CO'));
    if (!activeWorkers.length) return;

    this.workerDrafts.update(current => {
      const next = { ...current };
      activeWorkers.forEach((worker, index) => {
        next[worker.id] = swatches[index % swatches.length].hex;
      });
      return next;
    });
    this.errorMessage.set(null);
  }

  protected discardChanges(): void {
    this.errorMessage.set(null);
    this.resetConfirmOpen.set(false);
    this.syncDraftsFromTheme(this.theme(), this.workers());
  }

  protected saveChanges(): void {
    const changes = this.pendingChanges();
    if (!this.canEdit || this.saving() || this.hasErrors() || !changes.length) return;

    this.saving.set(true);
    this.errorMessage.set(null);
    this.resetConfirmOpen.set(false);

    this.colorTheme.saveBatch(changes).subscribe({
      next: savedTheme => {
        this.saving.set(false);
        this.syncDraftsFromTheme(savedTheme, this.workers());
        this.activePaletteId.set(null);
        this.toast.show('Colores guardados y aplicados en todo el sistema.');
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.errorMessage.set(getErrorMessage(err));
      }
    });
  }

  protected requestResetAll(): void {
    if (!this.canEdit || this.saving()) return;
    this.resetConfirmOpen.set(true);
    this.errorMessage.set(null);
  }

  protected cancelResetAll(): void {
    this.resetConfirmOpen.set(false);
  }

  protected confirmResetAll(): void {
    if (!this.canEdit || this.saving()) return;
    this.saving.set(true);
    this.errorMessage.set(null);

    this.colorTheme.resetAll().subscribe({
      next: resetTheme => {
        this.saving.set(false);
        this.resetConfirmOpen.set(false);
        this.activePaletteId.set(null);
        this.syncDraftsFromTheme(resetTheme, this.workers());
        this.toast.show('Colores restablecidos a los valores predeterminados.');
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.errorMessage.set(getErrorMessage(err));
      }
    });
  }

  private updateDraftMap(category: ColorCategory, key: string, value: string): void {
    if (category === 'FUEL') {
      this.fuelDrafts.update(map => ({ ...map, [key]: value }));
    } else if (category === 'SHIFT') {
      this.shiftDrafts.update(map => ({ ...map, [key]: value }));
    } else {
      this.workerDrafts.update(map => ({ ...map, [key]: value }));
    }
  }

  private syncDraftsFromTheme(theme: ColorThemeDto, workers: Worker[]): void {
    const fuels: Record<string, string> = {};
    for (const entry of theme.fuels) {
      fuels[entry.key] = entry.colorHex;
    }

    const shifts: Record<string, string> = {};
    for (const entry of theme.shifts) {
      shifts[entry.key] = entry.colorHex;
    }

    const workerMap: Record<string, string> = {};
    for (const worker of workers) {
      workerMap[worker.id] = theme.workers[worker.id.toLowerCase()] ?? theme.workers[worker.id] ?? '';
    }

    this.fuelDrafts.set(fuels);
    this.shiftDrafts.set(shifts);
    this.workerDrafts.set(workerMap);
  }
}
