import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Whole number that is changed with "−" and "+" between `min` and `max`. The number itself is a spin button:
 * it takes the arrows and Home / End from the keyboard.
 */
@Component({
  selector: 'app-number-stepper',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button type="button" tabindex="-1" aria-label="Menos" [disabled]="disabled() || value() <= min()"
      (click)="set(value() - 1)">−</button>
    <span class="value" role="spinbutton" [attr.tabindex]="disabled() ? -1 : 0" [attr.aria-label]="label()"
      [attr.aria-valuenow]="value()" [attr.aria-valuemin]="min()" [attr.aria-valuemax]="max()"
      [attr.aria-disabled]="disabled() || null" (keydown)="onKeydown($event)">{{ value() }}</span>
    <button type="button" tabindex="-1" aria-label="Más" [disabled]="disabled() || value() >= max()"
      (click)="set(value() + 1)">+</button>
  `,
  styleUrl: './number-stepper.component.css'
})
export class NumberStepperComponent {
  readonly value = input.required<number>();
  readonly min = input(0);
  readonly max = input(100);
  /** Accessible name of the number. */
  readonly label = input.required<string>();
  readonly disabled = input(false);
  readonly valueChange = output<number>();

  protected set(value: number): void {
    const next = Math.min(this.max(), Math.max(this.min(), value));
    if (!this.disabled() && next !== this.value()) this.valueChange.emit(next);
  }

  protected onKeydown(event: KeyboardEvent): void {
    const next: Record<string, number> = {
      ArrowUp: this.value() + 1,
      ArrowRight: this.value() + 1,
      ArrowDown: this.value() - 1,
      ArrowLeft: this.value() - 1,
      Home: this.min(),
      End: this.max()
    };
    if (!(event.key in next)) return;
    event.preventDefault();
    this.set(next[event.key]);
  }
}
