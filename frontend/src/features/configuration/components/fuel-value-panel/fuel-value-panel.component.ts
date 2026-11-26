import { ChangeDetectionStrategy, Component, ElementRef, computed, effect, inject, input, output, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { map } from 'rxjs';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { SpinnerComponent } from '../../../../atomic-design/atoms/spinner/spinner.component';
import { FormFieldComponent } from '../../../../atomic-design/molecules/form-field/form-field.component';
import { SegmentOption, SegmentedControlComponent } from '../../../../atomic-design/molecules/segmented-control/segmented-control.component';
import { SelectFieldComponent, SelectOption } from '../../../../atomic-design/molecules/select-field/select-field.component';
import { SidePanelComponent } from '../../../../atomic-design/organisms/side-panel/side-panel.component';
import { FuelPricesService } from '../../../../core/services/fuel-prices.service';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { newestFirst, valueOn } from '../../../../shared/helpers/dated-value.helpers';
import { CALENDAR_DAY, startDateValidator } from '../../../../shared/helpers/form-control.helpers';
import {
  FUELS, FUEL_VALUE_LABELS, formatFuelValue, formatFuelValueInput, formatGallonMoney, fuelName, gallonMargin,
  parseGallonValue
} from '../../../../shared/helpers/fuel-price.helpers';
import { isApiError } from '../../../../shared/helpers/problem-details.helpers';
import { FuelValueDto, FuelValueKind } from '../../../../shared/models/dtos/fuel-price.dto';
import { FuelType } from '../../../../shared/models/dtos/tank.dto';

/** What the panel was opened for: a new value (already pointing at a fuel and kind) or a saved one to change. */
export type FuelPanelRequest =
  | { mode: 'new'; fuelType: FuelType; kind: FuelValueKind }
  | { mode: 'edit'; kind: FuelValueKind; value: FuelValueDto };

export interface FuelValueSaved {
  kind: FuelValueKind;
  value: FuelValueDto;
  edited: boolean;
}

const SAVE_ERROR = 'No se pudo guardar. Revisa tu conexión e inténtalo de nuevo.';
const REJECTED_ERROR = 'No se pudo guardar. Revisa los datos e inténtalo de nuevo.';
const GONE_ERROR = 'Ese valor ya no está guardado. Cierra este panel y vuelve a intentarlo.';

function gallonValueValidator(control: AbstractControl<string>): ValidationErrors | null {
  const { error } = parseGallonValue(control.value);
  return error ? { gallonValue: error } : null;
}

/** Side panel to load a new sale price or purchase cost, or to change a saved one. */
@Component({
  selector: 'app-fuel-value-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule, ActionButtonDirective, FormFieldComponent, SegmentedControlComponent,
    SelectFieldComponent, SidePanelComponent, SpinnerComponent
  ],
  templateUrl: './fuel-value-panel.component.html',
  styleUrl: './fuel-value-panel.component.css'
})
export class FuelValuePanelComponent {
  private readonly service = inject(FuelPricesService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly open = input(false);
  readonly request = input<FuelPanelRequest | null>(null);
  readonly sales = input.required<FuelValueDto[]>();
  readonly purchases = input.required<FuelValueDto[]>();
  readonly today = input.required<string>();
  readonly closed = output<void>();
  readonly saved = output<FuelValueSaved>();
  /** The user wants to change the value that already starts on the chosen date. */
  readonly editRequested = output<FuelPanelRequest>();
  /** The server knows values the screen does not: the histories have to be read again. */
  readonly outdated = output<void>();

  protected readonly fuelOptions: SelectOption<FuelType>[] =
    FUELS.map(fuel => ({ value: fuel.type, label: fuel.name, color: fuel.color }));
  protected readonly kindOptions: SegmentOption<FuelValueKind>[] = [
    { value: 'sale', label: 'Precio de venta' },
    { value: 'purchase', label: 'Costo de compra' }
  ];

  protected readonly form = new FormGroup({
    fuelType: new FormControl<FuelType>('MOTOR'),
    price: new FormControl('', { nonNullable: true, validators: gallonValueValidator }),
    validFrom: new FormControl('', { nonNullable: true, validators: startDateValidator })
  });

  protected readonly kind = signal<FuelValueKind>('sale');
  protected readonly saving = signal(false);
  protected readonly generalError = signal('');
  /** Fuel, kind and date the server refused as repeated. */
  private readonly refusedKey = signal('');
  /** Value just saved: it must not count as a repeated date while the panel slides away. */
  private readonly savedId = signal<number | null>(null);

  private readonly values = toSignal(this.form.valueChanges.pipe(map(() => this.form.getRawValue())),
    { initialValue: this.form.getRawValue() });

  protected readonly editing = computed(() => {
    const request = this.request();
    return request?.mode === 'edit' ? request.value : null;
  });

  protected readonly heading = computed(() => this.editing() ? 'Editar valor' : 'Nuevo valor');

  private readonly fuelType = computed<FuelType>(() => this.values().fuelType ?? 'MOTOR');
  private readonly date = computed(() => CALENDAR_DAY.test(this.values().validFrom) ? this.values().validFrom : null);
  private readonly amount = computed(() => parseGallonValue(this.values().price).value);
  private readonly key = computed(() => `${this.kind()}|${this.fuelType()}|${this.date()}`);

  /** Saved values of the chosen fuel and kind, without the one this panel is writing. */
  private readonly others = computed(() => this.historyOf(this.kind())
    .filter(value => value.id !== this.editing()?.id && value.id !== this.savedId()));

  protected readonly duplicate = computed(() => {
    const date = this.date();
    const existing = date ? this.others().find(value => value.validFrom === date) : undefined;
    if (!existing) return null;
    return {
      source: existing,
      title: `Ya hay un ${FUEL_VALUE_LABELS[this.kind()]} de ${fuelName(this.fuelType())} desde el ${formatShortDate(existing.validFrom)}`,
      amount: formatFuelValue(this.kind(), existing.pricePerGallon),
      link: `Editar el del ${formatShortDate(existing.validFrom)}`
    };
  });

  /** The server said the date is taken but the reloaded history does not show by whom. */
  protected readonly refused = computed(() => !this.duplicate() && this.refusedKey() === this.key());

  /** Which value stops ruling on the chosen date, and until when the new one will rule. */
  protected readonly replacement = computed(() => {
    const date = this.date();
    if (!date || this.duplicate()) return '';
    const others = this.others();
    const before = valueOn(others, date);
    const after = newestFirst(others).filter(value => value.validFrom > date).at(-1);
    const parts: string[] = [];
    if (before) {
      parts.push(`Desde ese día reemplaza a ${formatFuelValue(this.kind(), before.pricePerGallon)} `
        + `(rige desde ${formatShortDate(before.validFrom)}).`);
    } else {
      parts.push('Antes de esa fecha no hay ningún valor cargado.');
    }
    if (after) parts.push(`Rige hasta que empiece el del ${formatShortDate(after.validFrom)}.`);
    return parts.join(' ');
  });

  /** Sale minus purchase on the chosen date with the typed value, next to the figure that rules today. */
  protected readonly preview = computed(() => {
    const date = this.date();
    const amount = this.amount();
    if (!date || amount === null || this.duplicate()) return null;
    const kind = this.kind();
    const otherKind: FuelValueKind = kind === 'sale' ? 'purchase' : 'sale';
    const other = valueOn(this.historyOf(otherKind), date)?.pricePerGallon;
    const todaySale = valueOn(this.historyOf('sale'), this.today())?.pricePerGallon;
    const todayPurchase = valueOn(this.historyOf('purchase'), this.today())?.pricePerGallon;
    const margin = other === undefined ? null
      : kind === 'sale' ? gallonMargin(amount, other) : gallonMargin(other, amount);
    return {
      when: date === this.today() ? 'Con este valor' : `El ${formatShortDate(date)}`,
      margin: margin === null ? null : formatGallonMoney(margin, true),
      negative: margin !== null && margin < 0,
      missing: `Falta un ${FUEL_VALUE_LABELS[otherKind]} a esa fecha para calcularlo.`,
      today: todaySale === undefined || todayPurchase === undefined
        ? 'hoy falta un valor'
        : `hoy ${formatGallonMoney(gallonMargin(todaySale, todayPurchase), true)}`
    };
  });

  protected readonly fuelLabel = computed(() => fuelName(this.fuelType()));

  constructor() {
    // Each new request starts the form over; it stays as it was while the panel slides away.
    effect(() => {
      const request = this.request();
      if (request) untracked(() => this.start(request));
    });
  }

  protected setKind(kind: FuelValueKind): void {
    this.kind.set(kind);
    this.generalError.set('');
  }

  /** Writes the figure the way it will be read, so a misplaced dot is noticed before saving. */
  protected tidyValue(): void {
    const control = this.form.controls.price;
    const { value } = parseGallonValue(control.value);
    if (value !== null) control.setValue(formatFuelValueInput(this.kind(), value));
  }

  protected editExisting(value: FuelValueDto): void {
    this.editRequested.emit({ mode: 'edit', kind: this.kind(), value });
  }

  protected close(): void {
    if (!this.saving()) this.closed.emit();
  }

  protected save(): void {
    if (this.saving() || this.duplicate()) return;
    this.form.controls.price.markAsDirty();
    this.form.controls.validFrom.markAsDirty();
    this.form.markAllAsTouched();
    const amount = this.amount();
    const { fuelType, validFrom } = this.form.getRawValue();
    if (this.form.invalid || amount === null || !fuelType) {
      this.focusFirstInvalid();
      return;
    }
    const kind = this.kind();
    const editing = this.editing();
    const body = { fuelType, pricePerGallon: amount, validFrom };
    const attempt = this.key();
    this.saving.set(true);
    this.generalError.set('');
    this.refusedKey.set('');
    const request = editing ? this.service.update(kind, editing.id, body) : this.service.create(kind, body);
    request.subscribe({
      next: value => {
        this.saving.set(false);
        this.savedId.set(value.id);
        this.saved.emit({ kind, value, edited: editing !== null });
      },
      error: (error: unknown) => {
        this.saving.set(false);
        const status = isApiError(error) ? error.status : 0;
        if (status === 409) {
          this.refusedKey.set(attempt);
          this.outdated.emit();
        } else if (status === 404) {
          this.generalError.set(GONE_ERROR);
          this.outdated.emit();
        } else {
          this.generalError.set(status === 400 ? REJECTED_ERROR : SAVE_ERROR);
        }
      }
    });
  }

  private start(request: FuelPanelRequest): void {
    this.generalError.set('');
    this.refusedKey.set('');
    this.savedId.set(null);
    this.saving.set(false);
    this.kind.set(request.kind);
    const editing = request.mode === 'edit' ? request.value : null;
    this.form.reset({
      fuelType: request.mode === 'edit' ? request.value.fuelType : request.fuelType,
      price: editing ? formatFuelValueInput(request.kind, editing.pricePerGallon) : '',
      validFrom: editing ? editing.validFrom : this.today()
    });
    // A saved value stays with its fuel: changing it would move it to another history.
    if (editing) this.form.controls.fuelType.disable();
    else this.form.controls.fuelType.enable();
  }

  private historyOf(kind: FuelValueKind): FuelValueDto[] {
    const all = kind === 'sale' ? this.sales() : this.purchases();
    return all.filter(value => value.fuelType === this.fuelType());
  }

  private focusFirstInvalid(): void {
    const id = this.form.controls.price.invalid ? 'fuel-value-price' : 'fuel-value-date';
    this.host.nativeElement.querySelector<HTMLElement>(`#${id}`)?.focus();
  }
}
