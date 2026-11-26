import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Params, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { MonthStepperComponent } from '../../../../atomic-design/molecules/month-stepper/month-stepper.component';
import { PopoverComponent } from '../../../../atomic-design/molecules/popover/popover.component';
import { TabItem, TabsComponent } from '../../../../atomic-design/molecules/tabs/tabs.component';
import { FUELS } from '../../../../shared/helpers/fuel-price.helpers';
import { isApiError } from '../../../../shared/helpers/problem-details.helpers';
import { monthOf, stationToday } from '../../../../shared/helpers/shift-display.helpers';
import { injectIsPhone } from '../../../../shared/helpers/viewport.helpers';
import { VariationFuelDetailComponent } from '../../components/variation-fuel-detail/variation-fuel-detail.component';
import { VariationSummaryComponent } from '../../components/variation-summary/variation-summary.component';
import { fuelDetailView } from '../../helpers/variation-detail.helpers';
import { monthAndYear, monthWord, variationFuelName } from '../../helpers/variation-format.helpers';
import {
  FUEL_SLUGS, fuelLevel, isEmptyMonth, rulesView, summaryView, tabTone
} from '../../helpers/variation-summary.helpers';
import { MonthlyVariationDto } from '../../models/monthly-variation.dto';
import { MonthlyVariationService } from '../../services/monthly-variation.service';

type Status = 'loading' | 'ready' | 'empty' | 'error';

const SUMMARY_TAB = 'resumen';
const MONTH = /^20\d{2}-(0[1-9]|1[0-2])$/;

/**
 * Resultados › Variación: whether what the tanks hold matches what should be there after the sales. The address
 * keeps the month and the tab (`?month=2026-04&tab=diesel`).
 */
@Component({
  selector: 'app-variation-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink, ActionButtonDirective, SkeletonComponent, EmptyStateComponent, MonthStepperComponent, PopoverComponent,
    TabsComponent, VariationSummaryComponent, VariationFuelDetailComponent
  ],
  templateUrl: './variation-page.component.html',
  styleUrl: './variation-page.component.css'
})
export class VariationPageComponent {
  private readonly service = inject(MonthlyVariationService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private readonly today = stationToday();
  protected readonly currentMonth = monthOf(this.today);
  protected readonly phone = injectIsPhone();
  protected readonly skeletonCards = [0, 1, 2, 3];

  protected readonly month = signal(this.currentMonth);
  protected readonly tab = signal(SUMMARY_TAB);
  protected readonly status = signal<Status>('loading');
  private readonly report = signal<MonthlyVariationDto | null>(null);
  private requested: string | null = null;
  private subscription: Subscription | null = null;

  protected readonly tabs = computed<TabItem[]>(() => {
    const report = this.report();
    return [
      { id: SUMMARY_TAB, label: this.phone() ? 'Resumen' : 'Resumen del mes' },
      ...FUELS.map(({ type }) => {
        const fuel = report?.fuels.find(item => item.fuelType === type);
        const tone = fuel && this.status() === 'ready' ? tabTone(fuelLevel(fuel)) : undefined;
        return { id: FUEL_SLUGS[type], label: variationFuelName(type), tone };
      })
    ];
  });

  protected readonly rules = computed(() => {
    const report = this.report();
    return report ? rulesView(report) : null;
  });

  protected readonly summary = computed(() => {
    const report = this.report();
    return report ? summaryView(report, this.today) : null;
  });

  /** The fuel of the open tab; null on the summary. */
  protected readonly detail = computed(() => {
    const report = this.report();
    const fuel = report?.fuels.find(item => FUEL_SLUGS[item.fuelType] === this.tab());
    return report && fuel ? fuelDetailView(fuel, report, this.today) : null;
  });

  protected readonly emptyHeading = computed(() => `${monthAndYear(this.month())} todavía no tiene mediciones ni cierres`);
  protected readonly errorHeading = computed(() => `No pudimos cargar la variación de ${monthWord(this.month())}`);

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe(params => {
      const tab = params.get('tab');
      this.tab.set(Object.values(FUEL_SLUGS).find(slug => slug === tab) ?? SUMMARY_TAB);
      const asked = params.get('month');
      const month = asked && MONTH.test(asked) ? asked : this.currentMonth;
      if (month === this.requested) return;
      this.requested = month;
      this.month.set(month);
      this.load();
    });
    inject(DestroyRef).onDestroy(() => this.subscription?.unsubscribe());
  }

  protected selectMonth(month: string): void {
    this.navigate({ month });
  }

  protected selectTab(tab: string): void {
    this.navigate({ tab: tab === SUMMARY_TAB ? null : tab });
  }

  protected load(): void {
    this.subscription?.unsubscribe();
    this.status.set('loading');
    this.report.set(null);
    this.subscription = this.service.month(this.month()).subscribe({
      next: report => {
        this.report.set(report);
        this.status.set(isEmptyMonth(report, this.today) ? 'empty' : 'ready');
      },
      // A month before any alert rule answers 404: there is nothing to show for it either.
      error: (error: unknown) => this.status.set(isApiError(error) && error.status === 404 ? 'empty' : 'error')
    });
  }

  private navigate(queryParams: Params): void {
    void this.router.navigate([], {
      relativeTo: this.route, queryParams, queryParamsHandling: 'merge', replaceUrl: true
    });
  }
}
