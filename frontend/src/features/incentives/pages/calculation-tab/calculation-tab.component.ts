import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Subscription } from 'rxjs';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import {
  SelectFieldComponent, SelectOption
} from '../../../../atomic-design/molecules/select-field/select-field.component';
import {
  addMonths, capitalize, monthLabel, monthOf, stationToday
} from '../../../../shared/helpers/shift-display.helpers';
import { MonthSummaryComponent } from '../../components/month-summary/month-summary.component';
import { PendingDaysPanelComponent } from '../../components/pending-days-panel/pending-days-panel.component';
import { UnresolvedUnitsPanelComponent } from '../../components/unresolved-units-panel/unresolved-units-panel.component';
import {
  VendorIncentivesTableComponent
} from '../../components/vendor-incentives-table/vendor-incentives-table.component';
import { INCENTIVES_START_MONTH, MonthResult, PendingDay } from '../../models/incentive-calculation.model';
import { AdditiveSalesService } from '../../services/additive-sales.service';
import { IncentiveCalculationService } from '../../services/incentive-calculation.service';

type CalculationView = 'loading' | 'invalid' | 'error' | 'empty' | 'ready';

/** "agosto" from "2026-08". */
function monthName(month: string): string {
  return monthLabel(month).split(' ')[0];
}

/** "Agosto 2026", with "· hasta hoy" for the month in progress. */
function optionLabel(month: string, current: string): string {
  const [name, , year] = monthLabel(month).split(' ');
  return `${capitalize(name)} ${year}${month === current ? ' · hasta hoy' : ''}`;
}

/** Every month from the start of the calculation to the current one, the newest first. */
function monthOptions(current: string): SelectOption<string>[] {
  const options: SelectOption<string>[] = [];
  for (let month = current; month >= INCENTIVES_START_MONTH; month = addMonths(month, -1)) {
    options.push({ value: month, label: optionLabel(month, current) });
  }
  return options;
}

/** "Cálculo del mes" tab: what each seller earns for the additives sold in a month. */
@Component({
  selector: 'app-calculation-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ActionButtonDirective, EmptyStateComponent, SelectFieldComponent, MonthSummaryComponent,
    PendingDaysPanelComponent, UnresolvedUnitsPanelComponent, VendorIncentivesTableComponent
  ],
  templateUrl: './calculation-tab.component.html',
  styleUrl: './calculation-tab.component.css'
})
export class CalculationTabComponent {
  private readonly calculation = inject(IncentiveCalculationService);
  private readonly additiveSales = inject(AdditiveSalesService);
  private readonly destroyRef = inject(DestroyRef);

  /** Only the administrator exists today; with more roles this hides the reprocess buttons. */
  readonly canReprocess = input(true);

  private readonly currentMonth = monthOf(stationToday());
  protected readonly monthControl = new FormControl<string | null>(this.currentMonth);
  protected readonly monthOptions = monthOptions(this.currentMonth);

  /** Month on screen, "yyyy-MM". */
  protected readonly month = signal(this.currentMonth);
  /** Answer for the month on screen; null while it loads. */
  private readonly result = signal<MonthResult | null>(null);
  private readonly pendingDays = signal<PendingDay[]>([]);
  private loadSubscription: Subscription | null = null;

  protected readonly monthName = computed(() => monthName(this.month()));
  protected readonly isCurrentMonth = computed(() => this.month() === this.currentMonth);

  protected readonly view = computed<CalculationView>(() => {
    const result = this.result();
    if (!result) return 'loading';
    if (result.outcome === 'INVALID_MONTH') return 'invalid';
    if (result.outcome === 'ERROR') return 'error';
    return result.data.vendors.length === 0 ? 'empty' : 'ready';
  });

  protected readonly errorMessage = computed(() => {
    const result = this.result();
    return result && result.outcome !== 'OK' ? result.message : '';
  });

  protected readonly data = computed(() => {
    const result = this.result();
    return result?.outcome === 'OK' ? result.data : null;
  });
  protected readonly vendors = computed(() => this.data()?.vendors ?? []);
  protected readonly total = computed(() => this.vendors().reduce((sum, vendor) => sum + vendor.monthTotal, 0));
  protected readonly unresolved = computed(() => this.vendors().flatMap(vendor => vendor.unresolved));

  /** Pending days that weigh on what is on screen: the ones of the month and of the months before it. */
  protected readonly visiblePendingDays = computed(() =>
    this.pendingDays().filter(day => monthOf(day.date) <= this.month()));

  constructor() {
    this.monthControl.valueChanges.pipe(takeUntilDestroyed()).subscribe(month => {
      if (month) this.load(month);
    });
    this.load(this.currentMonth);
    this.loadPendingDays();
  }

  protected retry(): void {
    this.load(this.month());
    this.loadPendingDays();
  }

  protected showCurrentMonth(): void {
    this.monthControl.setValue(this.currentMonth);
  }

  /** A day was reprocessed: the totals are asked again without clearing the screen. */
  protected refresh(): void {
    const month = this.month();
    this.calculation.getMonth(month).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(result => {
      if (result.outcome === 'OK' && this.month() === month) this.result.set(result);
    });
  }

  private load(month: string): void {
    this.loadSubscription?.unsubscribe();
    this.month.set(month);
    this.result.set(null);
    this.loadSubscription = this.calculation.getMonth(month).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(result => {
        this.result.set(result);
        // The selector shows no month while the one asked for cannot be calculated.
        if (result.outcome === 'INVALID_MONTH') this.monthControl.setValue(null, { emitEvent: false });
      });
  }

  /** Without the list the screen just shows no notice; the month and its totals do not depend on it. */
  private loadPendingDays(): void {
    this.additiveSales.listPending().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: days => this.pendingDays.set(days),
      error: () => undefined
    });
  }
}
