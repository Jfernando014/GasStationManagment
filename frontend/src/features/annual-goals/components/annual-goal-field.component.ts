import { Component, input } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { getControlErrorMessage } from '../../../shared/helpers/form-control.helpers';

/** Decimal field with the annual-goals prototype hierarchy and gallon suffix. */
@Component({
  selector: 'app-annual-goal-field',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <label class="goals-input-title" [for]="fieldId()">{{ label() }} *</label>
    <p class="goals-input-sub" [id]="fieldId() + '-subtitle'">{{ subtitle() }}</p>
    <div class="input-with-suffix">
      <input class="flat-input tnum" type="text" inputmode="decimal" autocomplete="off"
        [id]="fieldId()" [formControl]="control()" [class.is-invalid]="errorMessage()"
        [attr.aria-required]="!control().disabled" [attr.aria-invalid]="errorMessage() ? true : null"
        [attr.aria-describedby]="fieldId() + '-subtitle ' + fieldId() + '-caption' + (errorMessage() ? ' ' + fieldId() + '-error' : '')">
      <span class="input-suffix">Gal</span>
    </div>
    <p class="goals-input-caption" [id]="fieldId() + '-caption'">{{ caption() }}</p>
    @if (errorMessage(); as message) {
      <p class="field-error" [id]="fieldId() + '-error'">{{ message }}</p>
    }
  `,
  styles: `
    :host { display: block; }
    label { display: block; }
    input { font-size: 18px; font-weight: 800; padding-right: 52px; }
    input:disabled { color: var(--text-heading); opacity: 1; cursor: default; }
  `,
  host: { class: 'goals-input-box' }
})
export class AnnualGoalFieldComponent {
  readonly fieldId = input.required<string>();
  readonly label = input.required<string>();
  readonly subtitle = input.required<string>();
  readonly caption = input.required<string>();
  readonly control = input.required<FormControl<string>>();

  errorMessage(): string | null { return getControlErrorMessage(this.control()); }
}
