import {
  ChangeDetectionStrategy, Component, ElementRef, Injector, afterNextRender, computed, effect, inject, input,
  output, signal, viewChild
} from '@angular/core';

import { ConfirmDialogComponent } from '../../../../atomic-design/molecules/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../../core/services/toast.service';
import { formatCop, formatLongDate, formatShortDate } from '../../../../shared/helpers/format.helpers';
import {
  addMonths, firstDayOfMonth, monthOf, prefersReducedMotion, stationToday
} from '../../../../shared/helpers/shift-display.helpers';
import {
  AdditiveIncentiveRow, CreateIncentiveRuleResult, MAX_INCENTIVE_PER_UNIT, MAX_INCENTIVE_REASON_LENGTH
} from '../../models/additive-incentive.model';
import { AdditiveIncentivesService } from '../../services/additive-incentives.service';

/** A piece of a sentence; the amounts go in bold. */
interface TextPart {
  text: string;
  bold?: boolean;
}

type FieldName = 'additive' | 'value' | 'date';
type FieldErrors = Partial<Record<FieldName, string>>;

const VALUE_ERROR = 'El valor por unidad debe ser un número entero de pesos entre 0 y 100.000';
const NETWORK_ERROR = 'No se pudo guardar. Revisa tu conexión e inténtalo de nuevo.';
const CREATED_MESSAGE = 'Regla de incentivo creada';
const CALENDAR_DAY = /^\d{4}-\d{2}-\d{2}$/;
/** Typing waits this long before the "Pasa de … a …" sentence changes. */
const PREVIEW_DEBOUNCE_MS = 200;
/** Same as --duration-fast: the time the closing animation takes. */
const CLOSE_MS = 130;
/** More digits than the maximum are allowed while typing, so the range error can be shown. */
const MAX_TYPED_DIGITS = 7;

/** Where each backend error code shows its message. */
const ERROR_FIELD: Record<string, FieldName> = {
  INVALID_VALUE_PER_UNIT: 'value',
  VALID_FROM_NOT_AFTER_LAST_CLOSED: 'date',
  RULE_ALREADY_EXISTS: 'date',
  PRODUCT_INACTIVE: 'additive'
};

const FIELD_ORDER: FieldName[] = ['additive', 'value', 'date'];
const FIELD_IDS: Record<FieldName, string> = { additive: 'rule-additive', value: 'rule-value', date: 'rule-date' };

function thousands(digits: string): string {
  return digits.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

/** The rule that applies to an additive on a day, as far as the list knows: the current one or the scheduled one. */
function appliedOn(row: AdditiveIncentiveRow, day: string): { value: number; since: string } | null {
  if (row.nextValue !== null && row.nextSince && day >= row.nextSince) {
    return { value: row.nextValue, since: row.nextSince };
  }
  if (row.currentValue !== null) {
    return { value: row.currentValue, since: row.currentSince ?? '' };
  }
  return null;
}

/**
 * Side panel to create an incentive rule. It is a native modal `<dialog>` so the page behind is inert, Tab stays
 * inside, Escape asks to close and the focus goes back to the button that opened it.
 */
@Component({
  selector: 'app-new-rule-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ConfirmDialogComponent],
  templateUrl: './new-rule-panel.component.html',
  styleUrl: './new-rule-panel.component.css'
})
export class NewRulePanelComponent {
  private readonly service = inject(AdditiveIncentivesService);
  private readonly toast = inject(ToastService);
  private readonly injector = inject(Injector);

  /** Additive to start with, e.g. the one whose history is open. */
  readonly preselectedId = input<number | null>(null);
  /** The rule was created for this additive; the list reloads and highlights it. */
  readonly created = output<number>();
  /** The panel is gone (after its closing animation). */
  readonly closed = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  private readonly panel = viewChild.required<ElementRef<HTMLElement>>('panel');

  protected readonly maxReasonLength = MAX_INCENTIVE_REASON_LENGTH;
  protected readonly fieldIds = FIELD_IDS;
  protected readonly valueError = VALUE_ERROR;

  private readonly defaultDate = firstDayOfMonth(addMonths(monthOf(stationToday()), 1));
  private readonly initialAdditiveId = signal<number | null>(null);

  // Form
  protected readonly additiveId = signal<number | null>(null);
  protected readonly valueText = signal('');
  protected readonly date = signal(this.defaultDate);
  protected readonly reason = signal('');

  // Flow
  protected readonly submitting = signal(false);
  protected readonly closing = signal(false);
  protected readonly confirmOpen = signal(false);
  protected readonly fieldErrors = signal<FieldErrors>({});
  protected readonly generalError = signal('');
  protected readonly noChange = signal<TextPart[] | null>(null);
  /** Additives the backend said are inactive: they leave the select. */
  private readonly excludedIds = signal<number[]>([]);

  protected readonly options = computed(() =>
    this.service.additives().filter(row => !this.excludedIds().includes(row.additiveId)));

  protected readonly selected = computed(() =>
    this.service.additives().find(row => row.additiveId === this.additiveId()) ?? null);

  protected readonly valueNumber = computed(() => {
    const digits = this.valueText().replace(/\D/g, '');
    return digits === '' ? null : Number(digits);
  });

  /** Range error of the value; empty while it is empty or valid. */
  protected readonly liveValueError = computed(() => {
    const value = this.valueNumber();
    return value !== null && value > MAX_INCENTIVE_PER_UNIT ? VALUE_ERROR : '';
  });

  private readonly dateValid = computed(() => CALENDAR_DAY.test(this.date()));
  protected readonly complete = computed(() => this.additiveId() !== null && this.valueNumber() !== null
    && this.liveValueError() === '' && this.dateValid());
  protected readonly canSubmit = computed(() => this.complete() && !this.submitting() && this.noChange() === null);

  protected readonly hint = computed(() => {
    const row = this.selected();
    if (!row) return '';
    const parts: string[] = [];
    if (!row.hasEverHadIncentive) parts.push('Sin incentivo');
    else if (row.currentValue !== null) parts.push(`Hoy paga ${formatCop(row.currentValue)}`);
    else parts.push('Hoy no paga incentivo');
    if (row.nextValue !== null && row.nextSince) {
      parts.push(`${formatCop(row.nextValue)} programado desde el ${formatShortDate(row.nextSince, false)}`);
    }
    if (row.salePrice !== null) parts.push(`se vende a ${formatCop(row.salePrice)}`);
    return parts.join(' · ');
  });

  /** "¿Seguro? …" shown while typing; it never blocks. */
  protected readonly warning = computed(() => {
    if (!this.complete()) return '';
    const row = this.selected();
    const value = this.valueNumber() ?? 0;
    const reasons: string[] = [];
    if (row?.salePrice != null && value * 2 > row.salePrice) {
      reasons.push(`el valor es más de la mitad del precio de venta (${formatCop(row.salePrice)})`);
    }
    if (!this.date().endsWith('-01')) reasons.push('la fecha no es día 1 del mes');
    if (reasons.length === 0) return '';
    const text = reasons.join(' y ');
    return `¿Seguro? ${text.charAt(0).toUpperCase()}${text.slice(1)}. Puedes crearla igual.`;
  });

  // The result sentence waits for typing to pause so it does not flicker on every key.
  private readonly settled = signal({ id: this.additiveId(), value: this.valueNumber(), date: this.date() });

  protected readonly preview = computed<TextPart[] | null>(() => {
    if (!this.complete() || this.noChange() !== null) return null;
    const { id, value, date } = this.settled();
    const row = this.service.additives().find(item => item.additiveId === id);
    if (!row || value === null || value > MAX_INCENTIVE_PER_UNIT || !CALENDAR_DAY.test(date)) return null;
    const day = formatShortDate(date);
    const before = appliedOn(row, date)?.value ?? null;
    if (value === 0) return [{ text: 'Deja de pagar incentivo el ' }, { text: day, bold: true }, { text: '.' }];
    if (before === value) {
      return [{ text: 'Ese día ya paga ' }, { text: formatCop(value), bold: true }, { text: ' por unidad: no se creará nada.' }];
    }
    if (before === null || before === 0) {
      return [{ text: 'Empieza a pagar ' }, { text: formatCop(value), bold: true },
        { text: ' por unidad el ' }, { text: day, bold: true }, { text: '.' }];
    }
    return [{ text: 'Pasa de ' }, { text: formatCop(before), bold: true }, { text: ' a ' },
      { text: formatCop(value), bold: true }, { text: ' por unidad el ' }, { text: day, bold: true }, { text: '.' }];
  });

  protected readonly previewKey = computed(() => this.preview()?.map(part => part.text).join('') ?? '');

  protected readonly dirty = computed(() => this.additiveId() !== this.initialAdditiveId()
    || this.valueText() !== '' || this.date() !== this.defaultDate || this.reason() !== '');

  constructor() {
    effect(onCleanup => {
      const snapshot = { id: this.additiveId(), value: this.valueNumber(), date: this.date() };
      const timer = setTimeout(() => this.settled.set(snapshot), PREVIEW_DEBOUNCE_MS);
      onCleanup(() => clearTimeout(timer));
    });

    afterNextRender(() => {
      const preselected = this.preselectedId();
      if (preselected !== null && this.service.additives().some(row => row.additiveId === preselected)) {
        this.additiveId.set(preselected);
        this.initialAdditiveId.set(preselected);
      }
      this.dialog().nativeElement.showModal();
      afterNextRender(() => this.focusField(this.additiveId() === null ? 'additive' : 'value'), { injector: this.injector });
    });
  }

  // ---- Fields

  protected onAdditive(event: Event): void {
    const id = Number((event.target as HTMLSelectElement).value);
    this.additiveId.set(Number.isNaN(id) ? null : id);
    this.clearError('additive');
  }

  protected onValue(event: Event): void {
    const input = event.target as HTMLInputElement;
    const caret = input.selectionStart ?? input.value.length;
    const digitsBeforeCaret = input.value.slice(0, caret).replace(/\D/g, '').length;
    const digits = input.value.replace(/\D/g, '').slice(0, MAX_TYPED_DIGITS).replace(/^0+(?=\d)/, '');
    const formatted = thousands(digits);
    this.valueText.set(formatted);
    input.value = formatted;
    let position = 0;
    let seen = 0;
    while (position < formatted.length && seen < digitsBeforeCaret) {
      if (/\d/.test(formatted[position])) seen++;
      position++;
    }
    input.setSelectionRange(position, position);
    this.clearError('value');
  }

  protected onDate(event: Event): void {
    this.date.set((event.target as HTMLInputElement).value);
    this.clearError('date');
  }

  protected onReason(event: Event): void {
    this.reason.set((event.target as HTMLTextAreaElement).value);
  }

  private clearError(field: FieldName): void {
    if (this.fieldErrors()[field]) this.fieldErrors.update(errors => ({ ...errors, [field]: undefined }));
    this.generalError.set('');
  }

  // ---- Submit

  protected submit(): void {
    if (!this.canSubmit()) return;
    const additiveId = this.additiveId() as number;
    const valuePerUnit = this.valueNumber() as number;
    const effectiveFrom = this.date();
    this.submitting.set(true);
    this.fieldErrors.set({});
    this.generalError.set('');
    this.service.createRule({ additiveId, valuePerUnit, effectiveFrom, reason: this.reason().trim() || undefined })
      .subscribe(result => {
        this.submitting.set(false);
        this.handleResult(result, additiveId, valuePerUnit, effectiveFrom);
      });
  }

  private handleResult(result: CreateIncentiveRuleResult, additiveId: number, value: number, day: string): void {
    switch (result.outcome) {
      case 'CREATED':
      case 'CREATED_WITH_WARNINGS':
        // The warnings the backend adds are the ones the form already showed while typing.
        this.toast.show(result.message || CREATED_MESSAGE);
        this.created.emit(additiveId);
        this.finishClosing();
        return;
      case 'NO_CHANGE':
        this.noChange.set(this.noChangeText(result.message, additiveId, value, day));
        afterNextRender(() => this.focusFooterPrimary(), { injector: this.injector });
        return;
      case 'ERROR': {
        const field = result.code ? ERROR_FIELD[result.code] : undefined;
        if (!field) {
          this.generalError.set(NETWORK_ERROR);
          return;
        }
        if (field === 'additive') {
          this.excludedIds.update(ids => [...ids, additiveId]);
          this.additiveId.set(null);
        }
        this.fieldErrors.set({ [field]: result.message });
        afterNextRender(() => this.focusField(field), { injector: this.injector });
      }
    }
  }

  private noChangeText(backendMessage: string, additiveId: number, value: number, day: string): TextPart[] {
    const row = this.service.additives().find(item => item.additiveId === additiveId);
    const applied = row ? appliedOn(row, day) : null;
    if (!applied || applied.value !== value || !applied.since) return [{ text: backendMessage }];
    return [{ text: 'Ya paga ' },
      { text: `${formatCop(value)} por unidad desde el ${formatLongDate(applied.since)}`, bold: true },
      { text: '. No se creó nada.' }];
  }

  /** "Cambiar datos": back to the form, with the data still there. */
  protected changeData(): void {
    this.noChange.set(null);
    afterNextRender(() => this.focusField('value'), { injector: this.injector });
  }

  // ---- Closing

  /** Close button, scrim, Escape: asks first when there is something to lose. */
  protected requestClose(): void {
    if (this.submitting() || this.closing()) return;
    if (this.dirty() && this.noChange() === null) {
      this.confirmOpen.set(true);
      return;
    }
    this.finishClosing();
  }

  protected discard(): void {
    this.confirmOpen.set(false);
    this.finishClosing();
  }

  protected keepEditing(): void {
    this.confirmOpen.set(false);
  }

  protected onScrimClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) this.requestClose();
  }

  protected onCancel(event: Event): void {
    event.preventDefault();
    this.requestClose();
  }

  private finishClosing(): void {
    if (this.closing()) return;
    this.closing.set(true);
    setTimeout(() => {
      this.dialog().nativeElement.close();
      this.closed.emit();
    }, prefersReducedMotion() ? 0 : CLOSE_MS);
  }

  // ---- Focus

  private focusField(field: FieldName): void {
    this.panel().nativeElement.querySelector<HTMLElement>(`#${FIELD_IDS[field]}`)?.focus();
  }

  private focusFooterPrimary(): void {
    this.panel().nativeElement.querySelector<HTMLElement>('.footer .primary')?.focus();
  }
}
