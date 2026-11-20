import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl } from '@angular/forms';

import { SearchFieldComponent } from '../../../../atomic-design/atoms/search-field/search-field.component';
import { SegmentedControlComponent, SegmentOption } from '../../../../atomic-design/molecules/segmented-control/segmented-control.component';
import { SelectFieldComponent } from '../../../../atomic-design/molecules/select-field/select-field.component';
import { PerformanceMeasure } from '../../../../shared/models/dtos/seller-performance.dto';
import { shortDate } from '../../helpers/performance-view.helpers';
import { HISTORY_START, PerformanceMode } from '../../models/performance-view.model';

/**
 * Filters of the page. They apply by themselves: there is no button to press. The component only says what changed;
 * the page decides when to ask the server again.
 */
@Component({
  selector: 'app-performance-filters',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [SegmentedControlComponent, SelectFieldComponent, SearchFieldComponent],
  templateUrl: './performance-filters.component.html',
  styleUrl: './performance-filters.component.css'
})
export class PerformanceFiltersComponent {
  private readonly destroyRef = inject(DestroyRef);

  readonly mode = input.required<PerformanceMode>();
  /** Day shown when the mode is "Día", `YYYY-MM-DD`. */
  readonly date = input.required<string>();
  /** Month shown when the mode is "Mes", `YYYY-MM`. */
  readonly month = input.required<string>();
  readonly measure = input.required<PerformanceMeasure>();
  readonly monthOptions = input.required<{ value: string; label: string }[]>();
  /** Last day that can be chosen: today. */
  readonly today = input.required<string>();
  /** Writes the search box; only the tab that lists sellers uses it. */
  readonly searchLabel = input('Buscar vendedor');

  readonly modeChange = output<PerformanceMode>();
  readonly dateChange = output<string>();
  readonly monthChange = output<string>();
  readonly measureChange = output<PerformanceMeasure>();
  readonly searchChange = output<string>();

  protected readonly modeOptions: SegmentOption<PerformanceMode>[] = [
    { value: 'dia', label: 'Día' }, { value: 'mes', label: 'Mes' }, { value: 'historial', label: 'Todo el historial' }
  ];
  protected readonly measureOptions: { value: PerformanceMeasure; label: string }[] = [
    { value: 'FUEL_GALLONS', label: 'Combustibles · galones' },
    { value: 'BASKET_COP', label: 'Lubricantes y otros · COP' }
  ];
  protected readonly start = HISTORY_START;
  protected readonly historyText = () => `${shortDate(HISTORY_START)} a hoy`;

  protected readonly monthControl = new FormControl<string | null>(null);
  protected readonly measureControl = new FormControl<PerformanceMeasure | null>(null);

  constructor() {
    // The controls follow the page (a link from the journal changes the day, for instance) without telling it back.
    effect(() => this.monthControl.setValue(this.month(), { emitEvent: false }));
    effect(() => this.measureControl.setValue(this.measure(), { emitEvent: false }));
    this.monthControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(value => {
      if (value && value !== this.month()) this.monthChange.emit(value);
    });
    this.measureControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(value => {
      if (value && value !== this.measure()) this.measureChange.emit(value);
    });
  }

  protected onDate(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    if (value && value !== this.date()) this.dateChange.emit(value);
  }
}
