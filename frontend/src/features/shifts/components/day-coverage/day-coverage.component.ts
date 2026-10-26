import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { segmentOnOperationalDay, segmentsLabel } from '../../../../shared/helpers/shift-display.helpers';
import { DayStaffMember } from '../../../../shared/models/dtos/day-staff.dto';

const AXIS_HOURS = ['06', '09', '12', '15', '18', '21', '00', '03', '06'];

/** Barras del día operativo (06:00 → 06:00): de un vistazo se ve quién cubre cada franja y dónde queda un hueco. */
@Component({
  selector: 'app-day-coverage',
  standalone: true,
  templateUrl: './day-coverage.component.html',
  styleUrl: './day-coverage.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DayCoverageComponent {
  readonly members = input.required<DayStaffMember[]>();
  /** Filas recién cambiadas por una novedad: se resaltan un momento. */
  readonly flash = input<ReadonlySet<string>>(new Set());

  readonly axisHours = AXIS_HOURS;

  readonly rows = computed(() =>
    this.members().map(m => ({
      workerId: m.workerId,
      name: m.fullName,
      support: m.support,
      bars: m.segments.map((s, i) => ({
        ...segmentOnOperationalDay(s),
        label: i === 0 ? `${m.workedCode} · ${segmentsLabel([s])}` : segmentsLabel([s])
      }))
    }))
  );
}
