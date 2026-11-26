import { Component, input } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { getControlErrorMessage } from '../../../shared/helpers/form-control.helpers';

/**
 * Label + text input + error text (or hint) bound to a reactive form control.
 * Projected content goes under the field, e.g. an error with a link; name its id in `describedBy`.
 */
@Component({
  selector: 'app-form-field',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './form-field.component.html',
  host: { class: 'form-item' }
})
export class FormFieldComponent {
  readonly fieldId = input.required<string>();
  readonly label = input.required<string>();
  readonly control = input.required<FormControl<string>>();
  readonly type = input('text');
  readonly required = input(false);
  readonly maxLength = input<number | null>(null);
  /** Virtual keyboard to show on phones, e.g. `numeric`. */
  readonly inputMode = input<'text' | 'numeric' | 'decimal'>('text');
  /**
   * When true, anything that is not a digit is removed as it is typed or pasted. The length limit is then
   * applied after cleaning (the native maxlength would cut a pasted "12.345.678..." before removing the dots).
   */
  readonly digitsOnly = input(false);
  readonly placeholder = input('');
  readonly hint = input('');
  /** Unit written inside the field, at its right end, e.g. "%". The label should name it too. */
  readonly suffix = input('');
  /** Lowest and highest value for `date`, `month` and `number` fields. */
  readonly min = input<string | null>(null);
  readonly max = input<string | null>(null);
  /** Marks the field as invalid for an error the control does not know about, e.g. a range between two fields. */
  readonly invalid = input(false);
  readonly autocomplete = input<string | null>(null);
  /** Accessible name of the input when the visible label is shared by several rows, e.g. "Fecha de observación 3". */
  readonly ariaLabel = input<string | null>(null);
  /** Id of a projected text that explains the field, used when it shows no error or hint of its own. */
  readonly describedBy = input<string | null>(null);

  errorMessage(): string | null {
    return getControlErrorMessage(this.control());
  }

  /** Native maxlength, except in digits-only mode where the limit is applied after cleaning. */
  nativeMaxLength(): number | null {
    return this.digitsOnly() ? null : this.maxLength();
  }

  /** Removes the characters that are not allowed, keeping the cursor where the user was typing. */
  onInput(event: Event): void {
    if (!this.digitsOnly()) {
      return;
    }
    const field = event.target as HTMLInputElement;
    const limit = this.maxLength() ?? undefined;
    const clean = field.value.replace(/\D/g, '').slice(0, limit);
    if (clean === field.value) {
      return;
    }
    const cursor = field.selectionStart ?? field.value.length;
    const digitsBeforeCursor = field.value.slice(0, cursor).replace(/\D/g, '').length;
    const newCursor = Math.min(digitsBeforeCursor, clean.length);
    field.value = clean;
    field.setSelectionRange(newCursor, newCursor);
    this.control().setValue(clean);
  }
}
