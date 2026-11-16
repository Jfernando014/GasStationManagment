import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal, untracked
} from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { SegmentedControlComponent } from '../../../../atomic-design/molecules/segmented-control/segmented-control.component';
import { SelectFieldComponent } from '../../../../atomic-design/molecules/select-field/select-field.component';
import { monthName, monthOptions } from '../../../../shared/helpers/month-options.helpers';
import { monthOf, stationToday } from '../../../../shared/helpers/shift-display.helpers';
import { OvertimeNoticeComponent } from '../../components/overtime-notice/overtime-notice.component';
import { OvertimeSummaryTableComponent } from '../../components/overtime-summary-table/overtime-summary-table.component';
import {
  fortnightButtonLabel, fortnightEnd, fortnightFromQuery, fortnightOf
} from '../../helpers/overtime-display.helpers';
import { LoadResult, OvertimeSummary, OVERTIME_START_MONTH } from '../../models/overtime.model';
import { OvertimeSheetService } from '../../services/overtime-sheet.service';

type SummaryView = 'loading' | 'error' | 'empty' | 'ready';

/** Personal › Horas extra: what each worker is paid in a fortnight. The month and fortnight live in the address. */
@Component({
  selector: 'app-overtime-summary-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink, ActionButtonDirective, EmptyStateComponent, SegmentedControlComponent, SelectFieldComponent,
    OvertimeNoticeComponent, OvertimeSummaryTableComponent
  ],
  templateUrl: './overtime-summary-page.component.html',
  styleUrl: './overtime-summary-page.component.css'
})
export class OvertimeSummaryPageComponent {
  private readonly service = inject(OvertimeSheetService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  private readonly today = stationToday();
  private readonly current = fortnightOf(this.today);
  private readonly query = toSignal(this.route.queryParamMap, { initialValue: this.route.snapshot.queryParamMap });

  private readonly period = computed(() => fortnightFromQuery(this.query(), this.today));
  protected readonly month = computed(() => this.period().month);
  protected readonly fortnight = computed(() => this.period().number);

  protected readonly monthControl = new FormControl<string | null>(this.month());
  protected readonly monthOptions = monthOptions(OVERTIME_START_MONTH, monthOf(this.today));
  protected readonly fortnightOptions = computed(() => [
    { value: 1 as 1 | 2, label: fortnightButtonLabel(this.month(), 1) },
    { value: 2 as 1 | 2, label: fortnightButtonLabel(this.month(), 2) }
  ]);

  /** Answer for the fortnight on screen; null while it loads. */
  private readonly result = signal<LoadResult<OvertimeSummary> | null>(null);
  private loadSubscription: Subscription | null = null;

  protected readonly view = computed<SummaryView>(() => {
    const result = this.result();
    if (!result) return 'loading';
    if (result.outcome !== 'OK') return 'error';
    return result.data.workers.length === 0 ? 'empty' : 'ready';
  });
  protected readonly summary = computed(() => {
    const result = this.result();
    return result?.outcome === 'OK' ? result.data : null;
  });
  protected readonly errorMessage = computed(() => {
    const result = this.result();
    return result && result.outcome !== 'OK' ? result.message : '';
  });

  /** "del 16 al 30 de noviembre": the days whose journals will bring hours to an empty fortnight. */
  protected readonly periodText = computed(() => `del ${fortnightButtonLabel(this.month(), this.fortnight())} de ${monthName(this.month())}`);
  protected readonly isFuture = computed(() =>
    fortnightEnd(this.month(), this.fortnight()) >= this.today);

  constructor() {
    this.monthControl.valueChanges.pipe(takeUntilDestroyed()).subscribe(month => {
      if (month && month !== this.month()) this.navigate(month, month === this.current.month ? this.current.number : 1);
    });
    effect(() => this.monthControl.setValue(this.month(), { emitEvent: false }));
    effect(() => {
      const month = this.month();
      const fortnight = this.fortnight();
      untracked(() => this.load(month, fortnight));
    });
  }

  protected selectFortnight(fortnight: 1 | 2): void {
    this.navigate(this.month(), fortnight);
  }

  protected retry(): void {
    this.load(this.month(), this.fortnight());
  }

  private navigate(month: string, fortnight: number): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: { mes: month, quincena: fortnight }, replaceUrl: true });
  }

  private load(month: string, fortnight: number): void {
    this.loadSubscription?.unsubscribe();
    this.result.set(null);
    this.loadSubscription = this.service.getSummary(month, fortnight).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(result => this.result.set(result));
  }
}
