import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

export interface SegmentOption<T> {
  value: T;
  label: string;
}

/**
 * Small group of buttons where exactly one is chosen (a quincena, "+ Más" / "− Menos"). Each one is a real button
 * with `aria-pressed`, so the group is read like a set of toggles and works with Tab and Enter or Space.
 */
@Component({
  selector: 'app-segmented-control',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="seg" role="group" [attr.aria-label]="label()">
      @for (option of options(); track option.value) {
        <button type="button" [class.is-on]="option.value === value()" [attr.aria-pressed]="option.value === value()"
          [disabled]="disabled()" (click)="select(option.value)">{{ option.label }}</button>
      }
    </div>
  `,
  styleUrl: './segmented-control.component.css'
})
export class SegmentedControlComponent<T extends string | number> {
  readonly options = input.required<SegmentOption<T>[]>();
  readonly value = input.required<T>();
  /** Accessible name of the group. */
  readonly label = input.required<string>();
  readonly disabled = input(false);
  readonly valueChange = output<T>();

  protected select(value: T): void {
    if (value !== this.value()) this.valueChange.emit(value);
  }
}
