import { ChangeDetectionStrategy, Component, ElementRef, computed, effect, inject, input, untracked } from '@angular/core';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { SelectFieldComponent, SelectOption } from '../../../../atomic-design/molecules/select-field/select-field.component';
import { prefersReducedMotion, segmentOnOperationalDay, segmentsLabel } from '../../../../shared/helpers/shift-display.helpers';
import {
  compactHoursLabel,
  MAX_SUPPORT_RANGES,
  quickSupportShifts,
  sameSegments,
  SUPPORT_END_HOURS,
  SUPPORT_START_HOURS
} from '../../../../shared/helpers/support-hours.helpers';
import { ShiftCode } from '../../../../shared/models/dtos/shift-catalog.dto';
import { SupportHoursDraft } from './support-hours-draft';

const AXIS_HOURS = ['06', '09', '12', '15', '18', '21', '00', '03', '06'];

function hourOptions(hours: number[]): SelectOption<number>[] {
  return hours.map(hour => ({ value: hour, label: `${String(hour).padStart(2, '0')}:00` }));
}

/**
 * Chooses the hours of a support shift: the standard shifts as one-tap options, one or two blocks that can be
 * adjusted, and a live preview on the bar of the operational day (06:00 → 06:00).
 */
@Component({
  selector: 'app-support-hours-picker',
  standalone: true,
  imports: [ActionButtonDirective, SelectFieldComponent],
  templateUrl: './support-hours-picker.component.html',
  styleUrl: './support-hours-picker.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SupportHoursPickerComponent {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly draft = input.required<SupportHoursDraft>();
  readonly catalog = input<ShiftCode[]>([]);
  readonly disabled = input(false);
  /** Prefix of the field ids, unique in the page. */
  readonly idPrefix = input.required<string>();

  protected readonly startOptions = hourOptions(SUPPORT_START_HOURS);
  protected readonly endOptions = hourOptions(SUPPORT_END_HOURS);
  protected readonly axisHours = AXIS_HOURS;

  protected readonly groups = computed(() => {
    this.draft().ranges();
    return [...this.draft().form.controls];
  });

  protected readonly canAdd = computed(() => this.groups().length < MAX_SUPPORT_RANGES);

  protected readonly quick = computed(() => {
    const chosen = this.draft().segments();
    return quickSupportShifts(this.catalog()).map(shift => ({
      code: shift.code,
      segments: shift.schedule.segments,
      hours: compactHoursLabel(shift.schedule.segments),
      selected: !!chosen && sameSegments(chosen, shift.schedule.segments)
    }));
  });

  /** Blocks already valid on their own, drawn while the rest is still being chosen. */
  protected readonly preview = computed(() => {
    const segments = this.draft().ranges().flatMap(({ startHour, endHour }) =>
      startHour !== null && endHour !== null && endHour > startHour ? [{ startHour, endHour }] : []
    );
    return {
      bars: segments.map(segmentOnOperationalDay),
      label: segments.length ? segmentsLabel(segments) : 'Elige las horas',
      total: segments.reduce((sum, s) => sum + s.endHour - s.startHour, 0)
    };
  });

  constructor() {
    // A new error can be above or below what is on screen: bring it into view
    effect(() => {
      if (!this.draft().attention()) return;
      untracked(() => setTimeout(() => this.host.nativeElement
        .querySelector('.field-error')
        ?.scrollIntoView({ block: 'nearest', behavior: prefersReducedMotion() ? 'auto' : 'smooth' })));
    });
  }
}
