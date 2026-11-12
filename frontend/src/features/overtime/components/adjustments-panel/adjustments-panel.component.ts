import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, Injector, afterNextRender, computed, inject, input,
  output, signal
} from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ValidationErrors, Validators } from '@angular/forms';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { FormFieldComponent } from '../../../../atomic-design/molecules/form-field/form-field.component';
import { SegmentedControlComponent } from '../../../../atomic-design/molecules/segmented-control/segmented-control.component';
import { SelectFieldComponent, SelectOption } from '../../../../atomic-design/molecules/select-field/select-field.component';
import { ToastService } from '../../../../core/services/toast.service';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { SERVER_ERROR_KEY } from '../../../../shared/helpers/form-control.helpers';
import { stationToday } from '../../../../shared/helpers/shift-display.helpers';
import { CATEGORY_HELP } from '../../helpers/overtime-display.helpers';
import {
  MAX_REASON_LENGTH, MIN_REASON_LENGTH, OVERTIME_CATEGORIES, OvertimeAdjustment, OvertimeCategory
} from '../../models/overtime.model';
import { OvertimeSheetService } from '../../services/overtime-sheet.service';

type Direction = 'ADD' | 'SUBTRACT';

interface AdjustmentView {
  id: number;
  direction: Direction;
  tag: string;
  tagVariant: 'green' | 'red';
  hours: string;
  category: OvertimeCategory;
  reason: string;
  who: string;
}

const DIRECTION_OPTIONS: { value: Direction; label: string }[] = [
  { value: 'ADD', label: '+ Más' },
  { value: 'SUBTRACT', label: '− Menos' }
];

const CATEGORY_OPTIONS: SelectOption<OvertimeCategory>[] = OVERTIME_CATEGORIES.map(category => ({
  value: category, label: `${category} · ${CATEGORY_HELP[category].name}`
}));

function positiveWholeNumber(control: AbstractControl<string>): ValidationErrors | null {
  return /^[1-9]\d*$/.test(control.value) ? null : { format: 'Las horas deben ser un número entero mayor que cero' };
}

function reasonLength(control: AbstractControl<string>): ValidationErrors | null {
  const length = control.value.trim().length;
  return length >= MIN_REASON_LENGTH && length <= MAX_REASON_LENGTH
    ? null : { length: `El motivo debe tener entre ${MIN_REASON_LENGTH} y ${MAX_REASON_LENGTH} caracteres` };
}

/** Manual adjustments of one worker in one fortnight: add or remove hours of a category, with a reason. */
@Component({
  selector: 'app-adjustments-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ActionButtonDirective, TagComponent, FormFieldComponent, SegmentedControlComponent, SelectFieldComponent],
  templateUrl: './adjustments-panel.component.html',
  styleUrl: './adjustments-panel.component.css'
})
export class AdjustmentsPanelComponent {
  private readonly service = inject(OvertimeSheetService);
  private readonly toast = inject(ToastService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly destroyRef = inject(DestroyRef);

  readonly adjustments = input.required<OvertimeAdjustment[]>();
  readonly workerId = input.required<string>();
  readonly month = input.required<string>();
  readonly fortnight = input.required<number>();
  /** Only the administrator exists today; with more roles this hides adding and removing. */
  readonly canManage = input(true);
  /** An adjustment was added or removed: the sheet is out of date. */
  readonly changed = output<void>();

  protected readonly directionOptions = DIRECTION_OPTIONS;
  protected readonly categoryOptions = CATEGORY_OPTIONS;
  protected readonly maxReasonLength = MAX_REASON_LENGTH;

  protected readonly formOpen = signal(false);
  protected readonly direction = signal<Direction>('ADD');
  protected readonly submitting = signal(false);
  protected readonly formError = signal('');
  /** Adjustment waiting for the user to confirm its removal, and the one being removed. */
  protected readonly confirmingId = signal<number | null>(null);
  protected readonly removingId = signal<number | null>(null);
  protected readonly removeError = signal('');

  protected readonly form = new FormGroup({
    category: new FormControl<OvertimeCategory | null>(null, Validators.required),
    hours: new FormControl('', { nonNullable: true, validators: [Validators.required, positiveWholeNumber] }),
    reason: new FormControl('', { nonNullable: true, validators: [Validators.required, reasonLength] })
  });

  protected readonly views = computed<AdjustmentView[]>(() => this.adjustments().map(item => ({
    id: item.id,
    direction: item.direction,
    tag: item.direction === 'ADD' ? '+ Más' : '− Menos',
    tagVariant: item.direction === 'ADD' ? 'green' : 'red',
    hours: `${item.hours} ${item.hours === 1 ? 'hora' : 'horas'}`,
    category: item.category,
    reason: item.reason,
    who: `Registrado por ${item.createdBy ?? 'un usuario'} · ${formatShortDate(stationToday(new Date(item.createdAt)))}`
  })));

  protected openForm(): void {
    this.formOpen.set(true);
    afterNextRender(() => this.host.nativeElement.querySelector<HTMLElement>('#adjustment-category')?.focus(),
      { injector: this.injector });
  }

  protected closeForm(): void {
    this.formOpen.set(false);
    this.form.reset({ category: null, hours: '', reason: '' });
    this.direction.set('ADD');
    this.formError.set('');
    afterNextRender(() => this.host.nativeElement.querySelector<HTMLElement>('.add-button')?.focus(),
      { injector: this.injector });
  }

  protected save(): void {
    this.formError.set('');
    for (const control of Object.values(this.form.controls)) {
      control.markAsDirty();
      control.markAsTouched();
    }
    if (this.form.invalid || this.submitting()) return;
    const { category, hours, reason } = this.form.getRawValue();
    this.submitting.set(true);
    this.service.createAdjustment(this.workerId(), this.month(), this.fortnight(), {
      category: category as OvertimeCategory, direction: this.direction(), hours: Number(hours), reason
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(result => {
      this.submitting.set(false);
      if (result.outcome === 'OK') {
        this.toast.show('Ajuste registrado');
        this.closeForm();
        this.changed.emit();
        return;
      }
      const control = result.field ? this.form.get(result.field) : null;
      if (control) {
        control.setErrors({ [SERVER_ERROR_KEY]: result.message });
      } else {
        this.formError.set(result.message);
      }
    });
  }

  protected askRemove(id: number): void {
    this.removeError.set('');
    this.confirmingId.set(id);
    afterNextRender(() => this.host.nativeElement.querySelector<HTMLElement>(`[data-adjustment="${id}"] .confirm button`)?.focus(),
      { injector: this.injector });
  }

  protected cancelRemove(id: number): void {
    this.confirmingId.set(null);
    afterNextRender(() => this.host.nativeElement.querySelector<HTMLElement>(`[data-adjustment="${id}"] .remove-button`)?.focus(),
      { injector: this.injector });
  }

  protected remove(id: number): void {
    this.removingId.set(id);
    this.service.deleteAdjustment(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(result => {
      this.removingId.set(null);
      if (result.outcome === 'OK') {
        this.confirmingId.set(null);
        this.toast.show('Ajuste eliminado');
        this.changed.emit();
      } else {
        this.removeError.set(result.message);
      }
    });
  }
}
