import { ChangeDetectionStrategy, Component, EventEmitter, Output, input } from '@angular/core';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { DayStaffNovelty } from '../../../../shared/models/dtos/day-staff.dto';

/** Una novedad lista para mostrar: el texto ya viene resuelto por la página. */
export interface NoveltyItem {
  novelty: DayStaffNovelty;
  label: string;
  summary: string;
  /** Se pliega mientras el servidor confirma la anulación. */
  folding: boolean;
  /** Pregunta "¿Anularla?" abierta en esta tarjeta. */
  asking: boolean;
}

/** Lista de las novedades del día, con la anulación en dos pasos. */
@Component({
  selector: 'app-novelty-list',
  standalone: true,
  imports: [ActionButtonDirective, ShiftChipComponent],
  templateUrl: './novelty-list.component.html',
  styleUrl: './novelty-list.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NoveltyListComponent {
  readonly items = input.required<NoveltyItem[]>();
  readonly editable = input(false);
  readonly errorMessage = input<string | null>(null);

  @Output() readonly askCancel = new EventEmitter<DayStaffNovelty>();
  @Output() readonly keep = new EventEmitter<void>();
  @Output() readonly confirmCancel = new EventEmitter<DayStaffNovelty>();
}
