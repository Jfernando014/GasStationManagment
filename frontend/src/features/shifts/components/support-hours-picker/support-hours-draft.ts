import { computed, signal } from '@angular/core';
import { FormArray, FormControl, FormGroup } from '@angular/forms';

import { SERVER_ERROR_KEY } from '../../../../shared/helpers/form-control.helpers';
import { getErrorMessage, getFieldErrors } from '../../../../shared/helpers/problem-details.helpers';
import {
  completeSegments,
  HourDraft,
  hourRangeProblems,
  MAX_SUPPORT_RANGES
} from '../../../../shared/helpers/support-hours.helpers';
import { ShiftScheduleSegment } from '../../../../shared/models/dtos/shift-catalog.dto';

export type HourRangeForm = FormGroup<{
  startHour: FormControl<number | null>;
  endHour: FormControl<number | null>;
}>;

const SEGMENT_FIELD = /^segments\[(\d+)\](?:\.(startHour|endHour))?$/;

function createRange(): HourRangeForm {
  return new FormGroup({
    startHour: new FormControl<number | null>(null),
    endHour: new FormControl<number | null>(null)
  });
}

/** The hours being chosen for a support shift, with their problems and the ones the server reported. */
export class SupportHoursDraft {
  readonly form = new FormArray<HourRangeForm>([createRange()]);
  readonly ranges = signal<HourDraft[]>(this.form.getRawValue());
  /** True once the user tried to save: only then the empty selectors are reported. */
  readonly tried = signal(false);
  /** Server error that belongs to no selector. */
  readonly serverError = signal<string | null>(null);
  readonly serverRowErrors = signal<string[]>([]);
  /** Changes every time there is something new to look at, so the view paints it and scrolls to it. */
  readonly attention = signal(0);

  readonly problems = computed(() => hourRangeProblems(this.ranges(), this.tried()));
  /** The blocks ready to send; null while something is missing or invalid. */
  readonly segments = computed(() => completeSegments(this.ranges()));

  constructor() {
    this.form.valueChanges.subscribe(() => {
      this.ranges.set(this.form.getRawValue());
      this.clearServerErrors();
    });
  }

  addRange(): void {
    if (this.form.length >= MAX_SUPPORT_RANGES) return;
    this.tried.set(false);
    this.form.push(createRange());
  }

  removeRange(index: number): void {
    this.form.removeAt(index);
  }

  /** Writes those hours in the selectors; they can still be adjusted. */
  fill(segments: ShiftScheduleSegment[]): void {
    const length = Math.max(1, Math.min(segments.length, MAX_SUPPORT_RANGES));
    while (this.form.length > length) this.form.removeAt(this.form.length - 1, { emitEvent: false });
    while (this.form.length < length) this.form.push(createRange(), { emitEvent: false });
    this.form.setValue(Array.from({ length }, (_, i) => ({
      startHour: segments[i]?.startHour ?? null,
      endHour: segments[i]?.endHour ?? null
    })));
  }

  reset(segments: ShiftScheduleSegment[] = []): void {
    this.fill(segments);
    this.form.markAsPristine();
    this.form.markAsUntouched();
    this.tried.set(false);
    this.attention.set(0);
  }

  /** Reports what is missing. Returns the blocks ready to send, or null. */
  validate(): ShiftScheduleSegment[] | null {
    this.tried.set(true);
    const segments = this.segments();
    if (!segments) this.attention.update(n => n + 1);
    return segments;
  }

  /**
   * Places every error of the server next to its selector or block, and the rest under the picker.
   * The fields in `handledElsewhere` already show their error in the form that holds the picker.
   */
  applyServerError(error: unknown, handledElsewhere: string[] = []): void {
    const fieldErrors = getFieldErrors(error);
    const rows: string[] = [];
    const general: string[] = [];
    for (const { field, message } of fieldErrors) {
      const match = SEGMENT_FIELD.exec(field);
      const group = match ? this.form.controls[Number(match[1])] : undefined;
      if (match && group && match[2]) {
        const control = group.controls[match[2] as 'startHour' | 'endHour'];
        control.setErrors({ [SERVER_ERROR_KEY]: message });
        control.markAsTouched();
      } else if (match && group) {
        rows[Number(match[1])] = message;
      } else if (!handledElsewhere.includes(field)) {
        general.push(message);
      }
    }
    if (!fieldErrors.length) general.push(getErrorMessage(error));
    this.serverRowErrors.set(rows);
    this.serverError.set(general.join(' ') || null);
    this.attention.update(n => n + 1);
  }

  /** The selectors have no validators of their own: any error on them came from the server. */
  clearServerErrors(): void {
    this.form.controls.forEach(group => Object.values(group.controls).forEach(control => control.setErrors(null)));
    this.serverError.set(null);
    this.serverRowErrors.set([]);
  }
}
