import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, catchError, merge, of, startWith, switchMap, tap } from 'rxjs';
import { ActionButtonDirective } from '../../../atomic-design/atoms/action-button/action-button.directive';
import { TagComponent } from '../../../atomic-design/atoms/tag/tag.component';
import { TabItem, TabsComponent } from '../../../atomic-design/molecules/tabs/tabs.component';
import { AvatarComponent } from '../../../atomic-design/atoms/avatar/avatar.component';
import { PercentBarComponent } from '../../../atomic-design/atoms/percent-bar/percent-bar.component';
import { EmptyStateComponent } from '../../../atomic-design/molecules/empty-state/empty-state.component';
import { SelectFieldComponent, SelectOption } from '../../../atomic-design/molecules/select-field/select-field.component';
import { SearchFieldComponent } from '../../../atomic-design/atoms/search-field/search-field.component';
import { SalesTargetsService } from '../../../core/services/sales-targets.service';
import { SellerPerformanceService } from '../../../core/services/seller-performance.service';
import { PerformanceQuery, SellerPerformanceResponse } from '../../../shared/models/dtos/seller-performance.dto';
import { addExact, difference, formatStatistics, monthRange } from '../../../shared/helpers/sales-statistics.helpers';
import { applicableTarget, complianceBarValue, performanceRangeError, performanceReason } from '../../../shared/helpers/seller-performance.helpers';
import { applyFieldErrors, getErrorMessage, isApiError } from '../../../shared/helpers/problem-details.helpers';
import { formatDecimal, monthlyDailyPreview, targetDecimalValidator, targetMargin } from '../../../shared/helpers/sales-target.helpers';
import { SalesTargetResponse } from '../../../shared/models/dtos/sales-target.dto';
import { AnnualGoalFieldComponent } from '../components/annual-goal-field.component';

function localToday(): string {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

@Component({
  selector: 'app-annual-goals-page',
  standalone: true,
  imports: [ReactiveFormsModule, ActionButtonDirective, TagComponent, TabsComponent, EmptyStateComponent, SelectFieldComponent, SearchFieldComponent, AnnualGoalFieldComponent, AvatarComponent, PercentBarComponent],
  templateUrl: './annual-goals-page.component.html',
  styleUrl: './annual-goals-page.component.css'
})
export class AnnualGoalsPageComponent {
  private readonly service = inject(SalesTargetsService);
  private readonly performanceService = inject(SellerPerformanceService);
  private readonly complianceRequests$ = new Subject<PerformanceQuery | null>();
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new Subject<void>();
  readonly today = localToday();
  readonly currentYear = Number(this.today.slice(0, 4));
  readonly yearControl = new FormControl<number | null>(this.currentYear, Validators.required);
  readonly year = signal(this.currentYear);
  readonly yearOptions: SelectOption<number>[] = Array.from({ length: 21 }, (_, index) => ({
    value: this.currentYear - 10 + index, label: String(this.currentYear - 10 + index)
  }));
  readonly monthControl = new FormControl(this.today.slice(5, 7), { nonNullable: true });
  private readonly monthValue = toSignal(this.monthControl.valueChanges, { initialValue: this.monthControl.value });
  readonly month = computed(() => Number(this.monthValue()));
  readonly periodLabel = computed(() => new Intl.DateTimeFormat('es-CO', { month: 'long', year: 'numeric' })
    .format(new Date(this.year(), this.month() - 1, 1)));
  readonly tab = signal<'cumplimiento' | 'metas'>('cumplimiento');
  readonly tabs: TabItem[] = [
    { id: 'cumplimiento', label: 'Cumplimiento' },
    { id: 'metas', label: 'Metas' }
  ];
  readonly periodMode = signal<'dia' | 'semana' | 'mes'>('mes');
  readonly complianceMonth = new FormControl(this.today.slice(0, 7), { nonNullable: true });
  readonly complianceDate = new FormControl(this.today, { nonNullable: true });
  readonly complianceTo = new FormControl(this.today, { nonNullable: true });
  readonly complianceForm = new FormGroup({ month: this.complianceMonth, date: this.complianceDate, to: this.complianceTo });
  private readonly complianceMonthValue = toSignal(this.complianceMonth.valueChanges, { initialValue: this.complianceMonth.value });
  private readonly complianceDateValue = toSignal(this.complianceDate.valueChanges, { initialValue: this.complianceDate.value });
  private readonly complianceToValue = toSignal(this.complianceTo.valueChanges, { initialValue: this.complianceTo.value });
  readonly sellerSearch = signal('');
  readonly compliance = signal<SellerPerformanceResponse | null>(null);
  readonly complianceLoading = signal(false);
  readonly complianceError = signal('');
  readonly complianceRange = computed(() => {
    if (this.periodMode() === 'mes') {
      try { return monthRange(this.complianceMonthValue()); } catch { return null; }
    }
    return { from: this.complianceDateValue(), to: this.periodMode() === 'dia' ? this.complianceDateValue() : this.complianceToValue() };
  });
  readonly complianceRangeError = computed(() => {
    const range = this.complianceRange();
    return range ? performanceRangeError(range.from, range.to) : 'Selecciona un mes válido.';
  });
  readonly complianceRows = computed(() => {
    const search = this.normalizeSearch(this.sellerSearch());
    return (this.compliance()?.compliance.sellers ?? []).map((seller, index) => ({
      ...seller, rank: seller.availability === 'AVAILABLE' ? index + 1 : null,
      applicableTarget: applicableTarget(seller), barValue: complianceBarValue(seller.percent),
      reached: seller.ratio !== null && !difference(seller.ratio.numerator, seller.ratio.denominator).startsWith('-')
    })).filter(seller => !search || this.normalizeSearch(`${seller.recordedName} ${seller.workerId}`).includes(search));
  });
  readonly complianceSales = computed(() => {
    const sellers = this.compliance()?.compliance.sellers;
    return sellers?.length ? sellers.reduce((sum, seller) => addExact(sum, seller.sales), '0') : null;
  });
  readonly pendingCoverage = computed(() => this.compliance()?.compliance.coverage.filter(day => day.status === 'PENDING' || day.status === 'ABSENT') ?? []);
  readonly complianceEmpty = computed(() => {
    const response = this.compliance();
    if (this.complianceError()) return { heading: 'No se pudo consultar el cumplimiento', description: 'Reintenta la consulta para obtener datos confirmados.' };
    if (response?.compliance.sellers.length) return { heading: 'Sin coincidencias', description: 'Cambia la búsqueda para ver los vendedores del período.' };
    if (response) return {
      heading: response.compliance.availability === 'NO_DATA' ? 'Sin datos de ventas' : 'Cumplimiento no disponible',
      description: 'Las jornadas cerradas se incluyen automáticamente, sin cargar datos adicionales.'
    };
    return { heading: 'Selecciona el período y consulta', description: 'Pulsa CONSULTAR para obtener las ventas y metas del período seleccionado.' };
  });
  readonly formatCompliance = formatStatistics;
  readonly reasonText = performanceReason;
  readonly targets = signal<SalesTargetResponse[]>([]);
  readonly annualTarget = computed(() => this.targets().find(target => target.year === this.year() && target.month === null && target.category === 'FUEL_AGGREGATE') ?? null);
  readonly monthlyFuel = computed(() => this.targets().find(target => target.year === this.year() && target.month === this.month() && target.category === 'FUEL_AGGREGATE') ?? null);
  readonly monthlyBasket = computed(() => this.targets().find(target => target.year === this.year() && target.month === this.month() && target.category === 'BASKET') ?? null);
  readonly complianceGoal = computed(() => {
    // Never use a monthly total as the goal of a day/week, or an annual value as a monthly goal.
    if (this.periodMode() !== 'mes') return null;
    const period = this.complianceMonthValue();
    return this.targets().find(target => target.year === Number(period.slice(0, 4)) &&
      target.month === Number(period.slice(5, 7)) && target.category === 'FUEL_AGGREGATE')?.value ?? null;
  });
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly ready = signal(false);
  readonly forbidden = signal(false);
  readonly loadError = signal('');
  readonly saveError = signal('');
  readonly success = signal('');
  readonly confirming = signal(false);
  readonly form = new FormGroup({
    value: new FormControl('', { nonNullable: true, validators: [Validators.required, targetDecimalValidator] }),
    maximumSale: new FormControl('', { nonNullable: true, validators: [Validators.required, targetDecimalValidator] })
  });
  private readonly formValue = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });
  readonly margin = computed(() => {
    this.formValue();
    const { value, maximumSale } = this.form.getRawValue();
    return targetMargin(value, maximumSale);
  });
  readonly dailyPreview = computed(() => {
    const target = this.monthlyFuel();
    return target ? monthlyDailyPreview(target.value, new Date(this.year(), this.month(), 0).getDate()) : null;
  });
  readonly canRegister = computed(() => this.ready() && !this.loading() && !this.saving() && !this.annualTarget() && !this.forbidden());
  readonly format = formatDecimal;

  constructor() {
    this.complianceRequests$.pipe(
      switchMap(query => query ? this.performanceService.read(query).pipe(catchError((error: unknown) => {
        this.complianceError.set(this.performanceErrorMessage(error));
        return of(null);
      })) : of(null)),
      takeUntilDestroyed()
    ).subscribe(report => {
      this.compliance.set(report);
      this.complianceLoading.set(false);
    });
    merge(this.complianceMonth.valueChanges, this.complianceDate.valueChanges, this.complianceTo.valueChanges)
      .pipe(takeUntilDestroyed()).subscribe(() => this.invalidateCompliance());
    this.consultCompliance();
    this.reload$.pipe(
      tap(() => {
        this.loading.set(true);
        this.ready.set(false);
        this.loadError.set('');
        this.form.disable();
      }),
      switchMap(() => this.service.list(this.year()).pipe(catchError((error: unknown) => {
        this.loadError.set(this.errorMessage(error));
        return of(null);
      }))),
      takeUntilDestroyed()
    ).subscribe(targets => {
      this.loading.set(false);
      if (targets === null) return;
      this.targets.set(targets);
      this.ready.set(true);
      const annual = this.annualTarget();
      if (annual) {
        this.form.reset({ value: annual.value, maximumSale: annual.maximumSale ?? '' });
        this.form.disable();
      } else if (!this.forbidden()) {
        this.form.enable();
      }
    });
    this.yearControl.valueChanges.pipe(startWith(this.currentYear), takeUntilDestroyed()).subscribe(year => {
      if (year === null || this.saving()) return;
      this.year.set(year);
      this.targets.set([]);
      this.form.reset({ value: '', maximumSale: '' });
      this.confirming.set(false);
      this.saveError.set('');
      this.success.set('');
      this.reload$.next();
    });
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.confirming.set(false));
  }

  setComplianceMode(mode: 'dia' | 'semana' | 'mes'): void {
    if (mode === this.periodMode()) return;
    this.invalidateCompliance();
    this.periodMode.set(mode);
  }

  consultCompliance(): void {
    this.invalidateCompliance();
    const range = this.complianceRange();
    if (!range || this.complianceRangeError()) return;
    this.complianceLoading.set(true);
    this.complianceRequests$.next({ ...range, measure: 'FUEL_GALLONS', outputScale: 2 });
  }

  private invalidateCompliance(): void {
    this.complianceError.set('');
    this.complianceRequests$.next(null);
  }

  private normalizeSearch(value: string): string {
    return value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLocaleLowerCase('es-CO');
  }

  private performanceErrorMessage(error: unknown): string {
    if (isApiError(error)) {
      if (error.status === 401) return 'La sesión no permite consultar cumplimiento (HTTP 401). Inicia sesión nuevamente.';
      if (error.status === 403) return 'Se requiere autorización de administrador para consultar cumplimiento (HTTP 403).';
      if (error.status === 404) return 'La API de cumplimiento no está disponible en este servidor (HTTP 404).';
      if (error.status === 502) return 'No se pudieron leer o validar las fuentes de cumplimiento (HTTP 502). Reintenta la consulta.';
    }
    return error instanceof Error ? error.message : getErrorMessage(error);
  }

  retry(): void { if (!this.saving()) this.reload$.next(); }

  requestSave(): void {
    this.form.markAllAsTouched();
    this.form.markAllAsDirty();
    if (this.canRegister() && this.form.valid) this.confirming.set(true);
  }

  confirmSave(): void {
    if (!this.confirming() || !this.canRegister() || !this.form.valid) return;
    const values = this.form.getRawValue();
    this.saving.set(true);
    this.confirming.set(false);
    this.saveError.set('');
    this.success.set('');
    this.form.disable();
    this.yearControl.disable();
    this.service.create({ year: this.year(), category: 'FUEL_AGGREGATE', unit: 'GALLONS', ...values })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: target => {
          // Lock immediately on the persisted POST response, even if the subsequent GET fails.
          this.targets.update(targets => [...targets.filter(row => row.id !== target.id), target]);
          this.success.set(target.message ?? 'Metas del año guardadas correctamente');
          this.saving.set(false);
          this.yearControl.enable({ emitEvent: false });
          this.reload$.next();
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.yearControl.enable({ emitEvent: false });
          this.saveError.set(this.errorMessage(error));
          if (isApiError(error) && (error.status === 401 || error.status === 403)) this.forbidden.set(true);
          // A conflict or lost POST response requires a fresh query before another registration.
          if (!isApiError(error) || error.status === 409 || error.status === 0 || error.status >= 500) {
            this.reload$.next();
          } else if (this.canRegister()) {
            this.form.enable();
          }
          applyFieldErrors(this.form, error);
        }
      });
  }

  private errorMessage(error: unknown): string {
    if (isApiError(error)) {
      if (error.status === 401) return 'No se pudo autenticar la solicitud (HTTP 401). Inicia sesión nuevamente para consultar o guardar metas.';
      if (error.status === 403) return 'El servidor rechazó la solicitud de metas (HTTP 403). Puede deberse a permisos insuficientes o a que la API de metas todavía no esté disponible en este backend. No se ha podido confirmar la causa.';
      if (error.status === 409) return 'Ya existe una meta para este año y categoría. Se consultará el registro guardado.';
      if (error.status === 404) return 'La API de metas no está disponible en este backend. No se ha simulado ningún guardado.';
    }
    return getErrorMessage(error);
  }
}
