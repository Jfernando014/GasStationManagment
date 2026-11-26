import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, ValidationErrors } from '@angular/forms';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { SpinnerComponent } from '../../../../atomic-design/atoms/spinner/spinner.component';
import { FormFieldComponent } from '../../../../atomic-design/molecules/form-field/form-field.component';
import { SegmentOption, SegmentedControlComponent } from '../../../../atomic-design/molecules/segmented-control/segmented-control.component';
import { formatCop } from '../../../../shared/helpers/format.helpers';
import { monthName } from '../../../../shared/helpers/month-options.helpers';
import { formatPercentage, formatPercentageInput, parsePercentage } from '../../../../shared/helpers/percentage.helpers';
import { isApiError } from '../../../../shared/helpers/problem-details.helpers';
import { commissionChange } from '../../helpers/fuel-profit.helpers';
import { CardPercentageMode, FuelProfitReportDto } from '../../models/fuel-profit.dto';
import { FuelProfitService } from '../../services/fuel-profit.service';

const SAVE_ERROR = 'No se pudo guardar. Revisa tu conexión e inténtalo de nuevo.';
const REJECTED_ERROR = 'No se pudo guardar. Revisa el porcentaje e inténtalo de nuevo.';
const FORBIDDEN_ERROR = 'Solo el administrador puede cambiar el % con tarjeta.';

function percentageValidator(control: AbstractControl<string>): ValidationErrors | null {
  const { error } = parsePercentage(control.value);
  return error ? { percentage: error } : null;
}

/** "% de ventas con tarjeta" of a month: the one the system calculates or one typed by the administrator. */
@Component({
  selector: 'app-card-percentage',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ActionButtonDirective, SpinnerComponent, FormFieldComponent, SegmentedControlComponent],
  templateUrl: './card-percentage.component.html',
  styleUrl: './card-percentage.component.css'
})
export class CardPercentageComponent {
  private readonly service = inject(FuelProfitService);

  readonly report = input.required<FuelProfitReportDto>();
  readonly canEdit = input(false);
  /** The report is being read again after a save. */
  readonly refreshing = input(false);
  /** The setting was saved. */
  readonly saved = output<void>();

  protected readonly modes: SegmentOption<CardPercentageMode>[] = [
    { value: 'REAL', label: 'Real' },
    { value: 'MANUAL', label: 'Manual' }
  ];

  protected readonly mode = signal<CardPercentageMode>('REAL');
  protected readonly control = new FormControl('', { nonNullable: true, validators: percentageValidator });
  private readonly typed = toSignal(this.control.valueChanges, { initialValue: this.control.value });
  protected readonly saving = signal(false);
  protected readonly error = signal('');

  protected readonly month = computed(() => monthName(this.report().month));
  protected readonly busy = computed(() => this.saving() || this.refreshing());
  private readonly percentage = computed(() => parsePercentage(this.typed()).value);

  protected readonly card = computed(() => {
    const card = this.report().card;
    return {
      real: card.realPercentage === null ? null : formatPercentage(card.realPercentage),
      cardSales: formatCop(card.cardSales),
      totalSales: formatCop(card.totalSales),
      note: card.realPercentageNote,
      savedManual: card.mode === 'MANUAL' && card.manualPercentage !== null
        ? formatPercentage(card.manualPercentage, 4) : null
    };
  });

  /** Something differs from what is saved. */
  protected readonly dirty = computed(() => {
    const card = this.report().card;
    if (this.mode() !== card.mode) return true;
    return this.mode() === 'MANUAL' && this.percentage() !== card.manualPercentage;
  });

  protected readonly canSave = computed(() => this.canEdit() && !this.busy() && this.dirty()
    && (this.mode() === 'REAL' || this.percentage() !== null));

  /** What the typed percentage would do to the commission; empty when it cannot be known for sure. */
  protected readonly preview = computed(() => {
    const value = this.percentage();
    if (this.mode() !== 'MANUAL' || value === null || !this.dirty()) return null;
    const change = commissionChange(this.report(), value);
    if (change === null) return null;
    const scope = this.report().totals.complete ? '' : ' en los combustibles completos';
    const percent = formatPercentage(value, 4);
    if (Math.abs(change) < 1) {
      return { start: `Con ${percent} la comisión de ${this.month()} queda igual`, amount: '', end: `${scope}.`, rises: false };
    }
    return {
      start: `Con ${percent} la comisión de ${this.month()} ${change > 0 ? 'sube' : 'baja'}`,
      amount: formatCop(Math.abs(change)),
      end: `${scope}.`,
      rises: change > 0
    };
  });

  constructor() {
    // Each report read, also after a save, starts the block over from what is saved.
    effect(() => {
      this.report();
      untracked(() => this.restore());
    });
    effect(() => {
      if (this.canEdit() && !this.busy()) this.control.enable();
      else this.control.disable();
    });
  }

  protected useReal(): void {
    const real = this.report().card.realPercentage;
    if (real !== null) {
      this.control.setValue(formatPercentageInput(real));
      this.control.markAsDirty();
    }
  }

  protected restore(): void {
    const card = this.report().card;
    this.mode.set(card.mode);
    this.control.reset(card.manualPercentage === null ? '' : formatPercentageInput(card.manualPercentage));
    this.error.set('');
  }

  protected save(): void {
    if (this.mode() === 'MANUAL') {
      this.control.markAsDirty();
      this.control.markAsTouched();
    }
    if (!this.canSave()) return;
    const mode = this.mode();
    this.saving.set(true);
    this.error.set('');
    this.service.saveCardSetting(this.report().month, {
      mode,
      manualPercentage: mode === 'MANUAL' ? this.percentage() : null
    }).subscribe({
      next: () => {
        this.saving.set(false);
        this.saved.emit();
      },
      error: (error: unknown) => {
        this.saving.set(false);
        const status = isApiError(error) ? error.status : 0;
        this.error.set(status === 403 ? FORBIDDEN_ERROR : status === 400 ? REJECTED_ERROR : SAVE_ERROR);
      }
    });
  }
}
