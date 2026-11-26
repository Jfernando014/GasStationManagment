import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { MonthStepperComponent } from '../../../../atomic-design/molecules/month-stepper/month-stepper.component';
import { TabItem, TabsComponent } from '../../../../atomic-design/molecules/tabs/tabs.component';
import { ToastComponent } from '../../../../atomic-design/molecules/toast/toast.component';
import { AuthService } from '../../../../core/services/auth.service';
import { SalesParametersService } from '../../../../core/services/sales-parameters.service';
import { ToastService } from '../../../../core/services/toast.service';
import { formatCop, formatShortDate } from '../../../../shared/helpers/format.helpers';
import { monthName } from '../../../../shared/helpers/month-options.helpers';
import { formatPercentage } from '../../../../shared/helpers/percentage.helpers';
import { isApiError } from '../../../../shared/helpers/problem-details.helpers';
import { monthOf, stationToday } from '../../../../shared/helpers/shift-display.helpers';
import { ProfitInputsComponent } from '../../components/profit-inputs/profit-inputs.component';
import { ProfitSummaryComponent } from '../../components/profit-summary/profit-summary.component';
import { commissionDifference, monthTitle } from '../../helpers/fuel-profit.helpers';
import { FuelProfitReportDto } from '../../models/fuel-profit.dto';
import { FuelProfitService } from '../../services/fuel-profit.service';

type View = 'resumen' | 'datos';
/** `no-rate`: there is no card commission in force for the month, so nothing can be calculated. */
type Status = 'loading' | 'ready' | 'error' | 'no-rate';

const MONTH = /^20\d{2}-(0[1-9]|1[0-2])$/;
const NO_RATE_CODE = 'SALES_PARAMETER_NOT_FOUND';

/**
 * Resultados › Ganancia: what each fuel earned in a month. The address keeps the month and the tab
 * (`?mes=2026-06&vista=datos`), so coming back from another screen shows the same.
 */
@Component({
  selector: 'app-profit-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ActionButtonDirective, SkeletonComponent, TagComponent, EmptyStateComponent, MonthStepperComponent,
    TabsComponent, ToastComponent, ProfitSummaryComponent, ProfitInputsComponent
  ],
  templateUrl: './profit-page.component.html',
  styleUrl: './profit-page.component.css'
})
export class ProfitPageComponent {
  private readonly service = inject(FuelProfitService);
  private readonly parameters = inject(SalesParametersService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly canEdit = inject(AuthService).isAdmin();
  protected readonly currentMonth = monthOf(stationToday());
  protected readonly tabs: TabItem[] = [
    { id: 'resumen', label: 'Resumen del mes' },
    { id: 'datos', label: 'Datos del cálculo' }
  ];
  protected readonly skeletonRows = [0, 1, 2, 3];

  protected readonly month = signal(this.currentMonth);
  protected readonly view = signal<View>('resumen');
  protected readonly status = signal<Status>('loading');
  protected readonly report = signal<FuelProfitReportDto | null>(null);
  protected readonly refreshing = signal(false);
  /** First month with a card commission: there is nothing to calculate before it. Null while unknown. */
  protected readonly firstMonth = signal<string | null>(null);
  /** The address asks for something that is not a month. */
  private readonly invalidMonth = signal(false);
  private requested: string | null = null;
  private subscription: Subscription | null = null;

  protected readonly empty = computed(() => this.report()?.daysWithData === 0);

  protected readonly chip = computed(() => {
    const report = this.report();
    if (this.status() !== 'ready' || !report) return null;
    if (report.daysWithData === 0) return { text: 'Sin ventas', tone: 'gray' as const };
    const open = report.month >= this.currentMonth;
    const state = open ? 'Mes en curso' : 'Mes cerrado';
    return {
      text: `${state} · datos al ${formatShortDate(report.referenceDate, false)}`,
      tone: open ? ('orange' as const) : ('green' as const)
    };
  });

  protected readonly errorHeading = computed(() => this.invalidMonth()
    ? 'No pudimos calcular la ganancia'
    : `No pudimos calcular la ganancia de ${monthName(this.month())}`);

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(params => {
      this.view.set(params.get('vista') === 'datos' ? 'datos' : 'resumen');
      const asked = params.get('mes');
      const key = asked ?? this.currentMonth;
      if (key === this.requested) return;
      this.requested = key;
      const valid = MONTH.test(key);
      this.invalidMonth.set(!valid);
      this.month.set(valid ? key : this.currentMonth);
      if (valid) {
        this.load();
      } else {
        this.subscription?.unsubscribe();
        this.status.set('error');
      }
    });

    // If this cannot be read, the button of the previous month simply stays enabled.
    this.parameters.history('credibanco-commission-rate').pipe(takeUntilDestroyed()).subscribe({
      next: values => this.firstMonth.set(values.map(value => monthOf(value.validFrom)).sort()[0] ?? null),
      error: () => this.firstMonth.set(null)
    });

    inject(DestroyRef).onDestroy(() => {
      this.subscription?.unsubscribe();
      this.toast.hide();
    });
  }

  protected selectMonth(month: string): void {
    this.toast.hide();
    this.navigate({ mes: month });
  }

  protected selectView(view: string): void {
    this.navigate({ vista: view === 'datos' ? 'datos' : null });
  }

  protected goToCurrentMonth(): void {
    this.selectMonth(this.currentMonth);
  }

  protected retry(): void {
    if (this.invalidMonth()) this.goToCurrentMonth();
    else this.load();
  }

  /** After a value changed: reads the month again without taking the screen away. */
  protected refresh(after?: (before: FuelProfitReportDto, report: FuelProfitReportDto) => void): void {
    const before = this.report();
    this.subscription?.unsubscribe();
    this.refreshing.set(true);
    this.subscription = this.service.report(this.month()).subscribe({
      next: report => {
        this.report.set(report);
        this.refreshing.set(false);
        if (before) after?.(before, report);
      },
      error: (error: unknown) => this.fail(error)
    });
  }

  protected onCardSettingSaved(): void {
    const saved = `Guardado.`;
    this.refresh((before, report) => {
      const { appliedPercentage } = report.card;
      const percentage = appliedPercentage === null ? 'el % real' : formatPercentage(appliedPercentage, 4);
      const change = commissionDifference(before, report);
      const scope = report.totals.complete ? 'la comisión' : 'la comisión de los combustibles completos';
      const effect = change === null || Math.abs(change) < 1
        ? ''
        : `: ${scope} ${change > 0 ? 'subió' : 'bajó'} ${formatCop(Math.abs(change))}`;
      this.toast.show(`${saved} ${monthTitle(report.month)} ahora usa ${percentage} con tarjeta${effect}.`);
    });
  }

  private load(): void {
    this.subscription?.unsubscribe();
    this.status.set('loading');
    this.report.set(null);
    this.refreshing.set(false);
    this.subscription = this.service.report(this.month()).subscribe({
      next: report => {
        this.report.set(report);
        this.status.set('ready');
      },
      error: (error: unknown) => this.fail(error)
    });
  }

  private fail(error: unknown): void {
    this.refreshing.set(false);
    const noRate = isApiError(error) && error.status === 404 && error.code === NO_RATE_CODE;
    this.status.set(noRate ? 'no-rate' : 'error');
  }

  private navigate(queryParams: Params): void {
    void this.router.navigate([], {
      relativeTo: this.route, queryParams, queryParamsHandling: 'merge', replaceUrl: true
    });
  }
}
