import { ChangeDetectionStrategy, Component, EventEmitter, Output, computed, effect, input } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { capitalize, longDateLabel, segmentsLabel } from '../../../../shared/helpers/shift-display.helpers';
import { ShiftCode } from '../../../../shared/models/dtos/shift-catalog.dto';
import { GridCell } from '../schedule-grid/schedule-grid.component';

export interface DayChoice {
  code: string;
  note: string | null;
  /** Ficha pulsada: desde aquí "vuela" hasta la celda. */
  origin: HTMLElement;
}

const NOTE_MAX = 255;

/** Contenido del panel de un día: fichas del catálogo válidas para el rol del vendedor y una nota opcional. */
@Component({
  selector: 'app-day-editor',
  standalone: true,
  imports: [ReactiveFormsModule, ShiftChipComponent],
  templateUrl: './day-editor.component.html',
  styleUrl: './day-editor.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DayEditorComponent {
  readonly workerName = input.required<string>();
  /** TITULAR o APOYO, según el registro del vendedor. */
  readonly roleName = input.required<string>();
  readonly date = input.required<string>();
  readonly cell = input.required<GridCell>();
  readonly catalog = input<ShiftCode[]>([]);
  readonly readOnly = input(false);
  readonly saving = input(false);
  readonly savingCode = input<string | null>(null);
  readonly errorMessage = input<string | null>(null);

  @Output() readonly chosen = new EventEmitter<DayChoice>();

  readonly note = new FormControl('', { nonNullable: true, validators: [Validators.maxLength(NOTE_MAX)] });
  readonly noteMax = NOTE_MAX;

  readonly dateLabel = computed(() => capitalize(longDateLabel(this.date())));
  readonly roleLabel = computed(() => (this.roleName() === 'APOYO' ? 'apoyo' : 'titular'));

  /** Solo se ofrece lo que el backend aceptaría: códigos activos del rol del vendedor y DESCANSO. */
  readonly options = computed(() =>
    this.catalog()
      .filter(c => c.active && (c.role === null || c.role.name === this.roleName()))
      .sort((a, b) => Number(a.code === 'DESCANSO') - Number(b.code === 'DESCANSO'))
      .map(c => ({ code: c.code, name: c.name, hours: segmentsLabel(c.schedule?.segments ?? []) }))
  );

  readonly currentHours = computed(() => {
    const code = this.cell().code;
    const entry = this.catalog().find(c => c.code === code);
    return entry ? segmentsLabel(entry.schedule?.segments ?? []) : '';
  });

  constructor() {
    effect(() => {
      this.note.setValue(this.cell().note ?? '');
      if (this.readOnly()) this.note.disable(); else this.note.enable();
    });
  }

  choose(code: string, event: Event): void {
    if (this.readOnly() || this.saving() || this.note.invalid) return;
    const origin = (event.currentTarget as HTMLElement).querySelector<HTMLElement>('app-shift-chip') ?? (event.currentTarget as HTMLElement);
    const note = this.note.value.trim();
    this.chosen.emit({ code, note: note || null, origin });
  }
}
