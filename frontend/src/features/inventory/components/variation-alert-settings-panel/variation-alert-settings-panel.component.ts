import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule } from '@angular/forms';
import { map, merge } from 'rxjs';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { SpinnerComponent } from '../../../../atomic-design/atoms/spinner/spinner.component';
import { ConfirmDialogComponent } from '../../../../atomic-design/molecules/confirm-dialog/confirm-dialog.component';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { ToastComponent } from '../../../../atomic-design/molecules/toast/toast.component';
import { WarningPanelComponent } from '../../../../atomic-design/molecules/warning-panel/warning-panel.component';
import { SidePanelComponent } from '../../../../atomic-design/organisms/side-panel/side-panel.component';
import { ToastService } from '../../../../core/services/toast.service';
import { VariationAlertSettingsService } from '../../../../core/services/variation-alert-settings.service';
import { newestFirst } from '../../../../shared/helpers/dated-value.helpers';
import { CALENDAR_DAY } from '../../../../shared/helpers/form-control.helpers';
import { formatInputDate } from '../../../../shared/helpers/format.helpers';
import { applyFieldErrors, getErrorMessage, isApiError } from '../../../../shared/helpers/problem-details.helpers';
import { monthOf, prefersReducedMotion, stationToday } from '../../../../shared/helpers/shift-display.helpers';
import { VariationAlertSettingResponse } from '../../../../shared/models/dtos/variation-alert-setting.dto';
import {
  RECOMMENDED_VALUES, SHORTAGE_RANGES, checkRangeOrder, coverageLabel, createVariationSettingForm, currentSetting,
  deleteMessage, monthCoverage, settingSummary, toFormValues, toRequest
} from '../../helpers/variation-alert-settings.helpers';
import { VariationAlertSettingFieldsComponent } from '../variation-alert-setting-fields/variation-alert-setting-fields.component';
import { VariationValidityItem, VariationValidityListComponent } from '../variation-validity-list/variation-validity-list.component';

const PHONE_QUERY = '(max-width: 640px)';
const GONE_ERROR = 'Esa vigencia ya no está guardada. Elige otra de la lista.';

/**
 * Configuración › Umbrales › Alertas de variación: the rules that classify the monthly fuel variation, each with the
 * date it applies from. On a phone the list fills the screen and the form opens as a bottom sheet.
 */
@Component({
  selector: 'app-variation-alert-settings-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    NgTemplateOutlet, ReactiveFormsModule, ActionButtonDirective, SkeletonComponent, SpinnerComponent,
    ConfirmDialogComponent, EmptyStateComponent, ToastComponent, WarningPanelComponent, SidePanelComponent,
    VariationAlertSettingFieldsComponent, VariationValidityListComponent
  ],
  templateUrl: './variation-alert-settings-panel.component.html',
  styleUrl: './variation-alert-settings-panel.component.css'
})
export class VariationAlertSettingsPanelComponent {
  private readonly service = inject(VariationAlertSettingsService);
  private readonly toast = inject(ToastService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly destroyRef = inject(DestroyRef);

  private readonly today = stationToday();
  protected readonly skeletonCards = [0, 1, 2];

  protected readonly status = signal<'loading' | 'ready' | 'error'>('loading');
  private readonly settings = signal<VariationAlertSettingResponse[]>([]);
  /** Setting in the form; null while a new one is written. */
  protected readonly editingId = signal<number | null>(null);
  protected readonly sheetOpen = signal(false);
  protected readonly saving = signal(false);
  protected readonly deleting = signal(false);
  protected readonly deleteTarget = signal<VariationAlertSettingResponse | null>(null);
  /** Saved setting that already starts on the date the server refused. */
  protected readonly conflict = signal<VariationAlertSettingResponse | null>(null);
  protected readonly generalError = signal('');
  protected readonly phone = signal(false);
  /** Setting that was in the form before "Nueva vigencia", where "Cancelar" goes back to. */
  private previousId: number | null = null;

  protected readonly form = createVariationSettingForm();
  protected readonly formValue = toSignal(this.form.events.pipe(map(() => this.form.getRawValue())),
    { initialValue: this.form.getRawValue() });

  private readonly ascending = computed(() => newestFirst(this.settings()).reverse());
  private readonly current = computed(() => currentSetting(this.settings(), this.today));
  protected readonly editing = computed(() =>
    this.settings().find(setting => setting.id === this.editingId()) ?? null);
  protected readonly isBase = computed(() =>
    this.editing() !== null && this.editing()?.id === this.ascending()[0]?.id);
  protected readonly busy = computed(() => this.saving() || this.deleting());

  protected readonly items = computed<VariationValidityItem[]>(() => {
    const ascending = this.ascending();
    return ascending.map((setting, index) => ({
      id: setting.id,
      date: formatInputDate(setting.validFrom),
      current: setting.id === this.current()?.id,
      base: index === 0,
      months: coverageLabel(monthCoverage(ascending, index, this.today)),
      summary: settingSummary(setting)
    })).reverse();
  });

  /** On a phone with the sheet closed, the list points at the setting that rules today. */
  protected readonly selectedId = computed(() =>
    this.phone() && !this.sheetOpen() ? this.current()?.id ?? null : this.editingId());

  protected readonly title = computed(() => {
    const editing = this.editing();
    if (!editing) return 'Nueva vigencia';
    return this.isBase() ? 'Configuración base' : `Vigencia del ${formatInputDate(editing.validFrom)}`;
  });

  protected readonly dateNote = computed(() => {
    const date = this.formValue().validFrom;
    const later = CALENDAR_DAY.test(date)
      ? this.ascending().find(setting => setting.id !== this.editingId() && setting.validFrom > date)
      : undefined;
    if (!later) return 'Desde este mes en adelante.';
    const next = formatInputDate(later.validFrom);
    return monthOf(later.validFrom) === monthOf(date)
      ? `Ese mes usará la del ${next}, que empieza después.`
      : `Desde este mes hasta que empiece la del ${next}.`;
  });

  protected readonly deleteHeading = computed(() =>
    `¿Eliminar la vigencia del ${formatInputDate(this.deleteTarget()?.validFrom ?? '')}?`);

  protected readonly deleteText = computed(() => {
    const ascending = this.ascending();
    const index = ascending.findIndex(setting => setting.id === this.deleteTarget()?.id);
    return deleteMessage(ascending, index, this.today);
  });

  protected readonly formatDate = formatInputDate;

  constructor() {
    this.watchPhone();
    // A message about what was refused stops being true as soon as that value changes.
    this.form.controls.validFrom.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.conflict.set(null));
    merge(...Object.values(this.form.controls).map(control => control.valueChanges))
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.generalError.set(''));
    this.destroyRef.onDestroy(() => this.toast.hide());
    this.load();
  }

  protected load(): void {
    this.status.set('loading');
    this.service.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: settings => {
        this.settings.set(settings);
        this.status.set('ready');
        this.select(this.current() ?? this.ascending().at(-1) ?? null);
      },
      error: () => this.status.set('error')
    });
  }

  protected choose(id: number): void {
    const setting = this.settings().find(item => item.id === id);
    if (!setting || this.busy()) return;
    this.select(setting);
    this.openSheet();
  }

  protected startNew(): void {
    if (this.busy()) return;
    this.previousId = this.editingId() ?? this.previousId;
    this.select(null);
    this.openSheet();
  }

  /** Fills the fields; nothing is saved until "Guardar". */
  protected useRecommended(): void {
    this.form.patchValue(toFormValues(RECOMMENDED_VALUES));
  }

  protected cancel(): void {
    if (this.busy()) return;
    if (this.phone()) {
      this.sheetOpen.set(false);
      return;
    }
    const back = this.editing() ?? this.settings().find(item => item.id === this.previousId) ?? this.current();
    this.select(back);
  }

  /** Escape also reaches the sheet while the confirmation is open over it: the sheet stays. */
  protected closeSheet(): void {
    if (!this.deleteTarget() && !this.busy()) this.sheetOpen.set(false);
  }

  protected save(): void {
    if (this.busy()) return;
    this.conflict.set(null);
    this.generalError.set('');
    for (const control of Object.values(this.form.controls)) control.markAsDirty();
    this.form.markAllAsTouched();
    const value = this.form.getRawValue();
    const order = checkRangeOrder(SHORTAGE_RANGES.map(range => value[range.field]));
    if (this.form.invalid || order.message) {
      this.reveal('.flat-input.is-invalid', true);
      return;
    }

    const id = this.editingId();
    const request = toRequest(value);
    const operation = id === null ? this.service.create(request) : this.service.update(id, request);
    this.saving.set(true);
    operation.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: saved => {
        this.saving.set(false);
        this.settings.update(list => [...list.filter(item => item.id !== saved.id), saved]);
        this.select(saved);
        this.sheetOpen.set(false);
        const date = formatInputDate(saved.validFrom);
        this.toast.show(id === null ? `Vigencia del ${date} creada.` : `Vigencia del ${date} guardada.`);
        this.refresh();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.showSaveError(error, request.validFrom, id);
      }
    });
  }

  protected requestDelete(): void {
    const editing = this.editing();
    if (!editing || this.isBase() || this.busy()) return;
    this.toast.hide();
    this.deleteTarget.set(editing);
  }

  protected cancelDelete(): void {
    this.deleteTarget.set(null);
  }

  protected confirmDelete(): void {
    const target = this.deleteTarget();
    if (!target) return;
    this.deleteTarget.set(null);
    this.generalError.set('');
    this.deleting.set(true);
    this.service.delete(target.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.deleting.set(false);
        const index = this.ascending().findIndex(item => item.id === target.id);
        // The previous setting is the one that takes over its months.
        const takesOver = this.ascending()[index - 1] ?? null;
        this.settings.update(list => list.filter(item => item.id !== target.id));
        this.select(takesOver);
        this.sheetOpen.set(false);
        this.toast.show(`Vigencia del ${formatInputDate(target.validFrom)} eliminada.`);
        this.refresh();
      },
      error: (error: unknown) => {
        this.deleting.set(false);
        const gone = isApiError(error) && error.status === 404;
        this.generalError.set(gone ? GONE_ERROR : getErrorMessage(error));
        if (gone) this.refresh();
        this.reveal('.banner');
      }
    });
  }

  /** Puts a saved setting in the form, or starts a new one from the values that rule today. */
  private select(setting: VariationAlertSettingResponse | null): void {
    this.editingId.set(setting?.id ?? null);
    this.form.reset({
      validFrom: setting?.validFrom ?? '',
      ...toFormValues(setting ?? this.current() ?? RECOMMENDED_VALUES)
    });
    this.conflict.set(null);
    this.generalError.set('');
  }

  private openSheet(): void {
    if (!this.phone()) return;
    this.toast.hide();
    this.sheetOpen.set(true);
  }

  private showSaveError(error: unknown, date: string, id: number | null): void {
    const status = isApiError(error) ? error.status : 0;
    if (status === 409) {
      const taken = () => this.settings().find(item => item.validFrom === date && item.id !== id) ?? null;
      const show = () => {
        const other = taken();
        if (other) this.conflict.set(other);
        else this.generalError.set(getErrorMessage(error));
        this.reveal('.banner');
      };
      // The setting that holds the date may have been created elsewhere after this list was read.
      if (taken()) show();
      else this.refresh(show);
      return;
    }
    if (status === 400 && applyFieldErrors(this.form, error)) {
      this.reveal('.flat-input.is-invalid', true);
      return;
    }
    this.generalError.set(status === 404 ? GONE_ERROR : getErrorMessage(error));
    if (status === 404) this.refresh();
    this.reveal('.banner');
  }

  /** Reads the list again without taking the screen away; if it fails, what is shown stays. */
  private refresh(then: () => void = () => undefined): void {
    this.service.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: settings => {
        this.settings.set(settings);
        if (this.editingId() !== null && !this.editing()) {
          const message = this.generalError();
          this.select(this.current());
          this.generalError.set(message);
        }
        then();
      },
      error: then
    });
  }

  /** Waits for the screen to be drawn and brings the element into view. */
  private reveal(selector: string, focus = false): void {
    setTimeout(() => {
      const element = this.host.nativeElement.querySelector<HTMLElement>(selector);
      element?.scrollIntoView?.({ behavior: prefersReducedMotion() ? 'auto' : 'smooth', block: 'nearest' });
      if (focus) element?.focus({ preventScroll: true });
    });
  }

  private watchPhone(): void {
    const media = typeof window === 'undefined' ? null : window.matchMedia?.(PHONE_QUERY) ?? null;
    if (!media) return;
    // Both events: some embedded browsers do not emit the `change` of the media query.
    const sync = () => {
      this.phone.set(media.matches);
      if (!media.matches) this.sheetOpen.set(false);
    };
    sync();
    media.addEventListener('change', sync);
    window.addEventListener('resize', sync);
    this.destroyRef.onDestroy(() => {
      media.removeEventListener('change', sync);
      window.removeEventListener('resize', sync);
    });
  }
}
