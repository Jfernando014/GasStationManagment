import { ChangeDetectionStrategy, Component, computed, input, linkedSignal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AlertChipComponent } from '../../../../atomic-design/atoms/alert-chip/alert-chip.component';
import { DayRow } from '../../helpers/variation-days.helpers';

/** Days shown before asking for the rest. */
const FIRST_DAYS = 12;

/** Day by day on a phone: one card per day, which unfolds to show the rest of its figures. */
@Component({
  selector: 'app-variation-day-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, AlertChipComponent],
  templateUrl: './variation-day-list.component.html',
  styleUrl: './variation-day-list.component.css'
})
export class VariationDayListComponent {
  readonly rows = input.required<DayRow[]>();

  /** Both start over when the days change, e.g. with another fuel. */
  protected readonly open = linkedSignal<DayRow[], string | null>({ source: this.rows, computation: () => null });
  private readonly all = linkedSignal<DayRow[], boolean>({ source: this.rows, computation: () => false });

  protected readonly shown = computed(() => this.all() ? this.rows() : this.rows().slice(0, FIRST_DAYS));
  protected readonly remaining = computed(() => this.rows().length - this.shown().length);

  protected toggle(date: string): void {
    this.open.set(this.open() === date ? null : date);
  }

  protected showAll(): void {
    this.all.set(true);
  }
}
