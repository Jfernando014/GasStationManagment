import { Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, forkJoin, of, startWith, Subject, switchMap, takeUntil, tap } from 'rxjs';
import { EmptyStateComponent } from '../../../atomic-design/molecules/empty-state/empty-state.component';
import { FormFieldComponent } from '../../../atomic-design/molecules/form-field/form-field.component';
import { StatisticsDialogComponent } from './statistics-dialog.component';
import { CompetitionService } from '../../../core/services/competition.service';
import { Competitor, FUELS, FuelType, PriceComparison, PriceSimulation } from '../../../shared/models/dtos/competition.dto';
import { difference, formatStatistics, validDate } from '../../../shared/helpers/sales-statistics.helpers';
import { priceValidator } from '../../../shared/helpers/statistics-contracts.helpers';
import { applyFieldErrors, getErrorMessage, isApiError } from '../../../shared/helpers/problem-details.helpers';
import { StatisticsWorkspace } from '../statistics-workspace';

interface MatrixRow { id: string; name: string; brand: string; location: string; own: boolean; prices: Record<FuelType, string | null> }

@Component({ selector: 'app-competition-statistics', standalone: true, imports: [FormsModule, ReactiveFormsModule, EmptyStateComponent, FormFieldComponent, StatisticsDialogComponent],
  templateUrl: './competition-statistics.component.html', styleUrls: ['../statistics.css', './competition-statistics.component.css'] })
export class CompetitionStatisticsComponent {
  private readonly api = inject(CompetitionService);
  private readonly workspace = inject(StatisticsWorkspace);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new Subject<void>();
  private readonly simulate$ = new Subject<void>();
  readonly fuels = FUELS;
  readonly fuel = signal<FuelType | 'ALL'>('ALL');
  readonly brand = signal('ALL');
  readonly competitors = signal<Competitor[]>([]);
  readonly comparison = signal<PriceComparison | null>(null);
  readonly simulation = signal<PriceSimulation | null>(null);
  readonly loading = signal(false);
  readonly simulating = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly formError = signal('');
  readonly simulationError = signal('');
  readonly success = signal('');
  readonly dialog = signal<'station' | 'observation' | null>(null);
  readonly format = formatStatistics;
  readonly brands = computed(() => [...new Set(this.competitors().filter(c => c.active).map(c => c.brand))].sort());
  readonly activeStations = computed(() => this.competitors().filter(c => c.active));
  readonly stationForm = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150), Validators.pattern(/\S/)] }),
    brand: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100), Validators.pattern(/\S/)] }),
    location: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(255), Validators.pattern(/\S/)] })
  });
  readonly observationForm = new FormGroup({
    competitorId: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(/^[1-9]\d*$/)] }),
    fuelType: new FormControl<FuelType>('MOTOR', { nonNullable: true }),
    pricePerGallon: new FormControl('', { nonNullable: true, validators: [Validators.required, priceValidator()] }),
    observedAt: new FormControl('', { nonNullable: true, validators: Validators.required })
  });
  readonly simulationForm = new FormGroup({
    fuelType: new FormControl<FuelType>('MOTOR', { nonNullable: true }),
    MOTOR: new FormControl('', { nonNullable: true, validators: priceValidator() }),
    DIESEL: new FormControl('', { nonNullable: true, validators: priceValidator() }),
    EXTRA: new FormControl('', { nonNullable: true, validators: priceValidator() }),
    MAX_PRO: new FormControl('', { nonNullable: true, validators: priceValidator() }),
    acquisitionCost: new FormControl('', { nonNullable: true, validators: priceValidator(true) })
  });
  readonly rows = computed<MatrixRow[]>(() => {
    const data = this.comparison(); if (!data) return [];
    const prices = (id: string | null): Record<FuelType, string | null> => Object.fromEntries(FUELS.map(f => {
      const record = data.fuels.find(value => value.fuelType === f.value);
      return [f.value, id === null ? record?.bellavistaPrice ?? null : record?.observations.find(o => o.competitorId === id)?.pricePerGallon ?? null];
    })) as Record<FuelType, string | null>;
    const own: MatrixRow = { id: 'bellavista', name: 'Primax EDS Bellavista', brand: 'Primax', location: 'Nuestra estación', own: true, prices: prices(null) };
    return [own, ...this.activeStations().map(c => ({ ...c, own: false, prices: prices(c.id) }))]
      .filter(c => (this.brand() === 'ALL' || c.brand === this.brand()) && (this.fuel() === 'ALL' || c.prices[this.fuel() as FuelType] !== null));
  });
  readonly latestVerification = computed(() => {
    const dates = this.comparison()?.fuels.flatMap(f => f.observations.map(o => o.observedAt)).sort();
    const latest = dates?.at(-1); return latest ? new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeZone: 'America/Bogota' }).format(new Date(latest)) : 'No disponible';
  });
  readonly rank = computed(() => {
    const result = this.simulation(); if (!result || !result.observations.length) return null;
    return 1 + result.observations.filter(o => difference(o.pricePerGallon, result.proposedPrice).startsWith('-')).length;
  });

  constructor() {
    this.reload$.pipe(startWith(undefined), tap(() => { this.loading.set(true); this.simulating.set(false); this.error.set(''); this.comparison.set(null); this.simulation.set(null); }),
      switchMap(() => forkJoin({ stations: this.api.list(), comparison: this.api.compare() }).pipe(catchError((error: unknown) => { this.error.set(this.message(error)); return of(null); }))),
      takeUntilDestroyed(this.destroyRef)).subscribe(result => {
        this.loading.set(false);
        if (result) {
          this.competitors.set(result.stations); this.comparison.set(result.comparison);
          for (const fuel of result.comparison.fuels) if (!this.simulationForm.controls[fuel.fuelType].dirty)
            this.simulationForm.controls[fuel.fuelType].setValue(fuel.bellavistaPrice ?? '', { emitEvent: false });
        }
      });
    this.simulationForm.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => { this.simulation.set(null); this.simulationError.set(''); this.simulating.set(false); });
    this.simulate$.pipe(switchMap(() => {
      const values = this.simulationForm.getRawValue(); this.simulating.set(true); this.simulationError.set(''); this.simulation.set(null);
      return this.api.simulate({ fuelType: values.fuelType, proposedPrice: values[values.fuelType], acquisitionCost: values.acquisitionCost.trim() || null,
        ...(this.comparison() ? { asOf: this.comparison()!.asOf } : {}) }).pipe(takeUntil(this.simulationForm.valueChanges), takeUntil(this.reload$), catchError((error: unknown) => { this.simulationError.set(this.message(error)); return of(null); }));
    }), takeUntilDestroyed(this.destroyRef)).subscribe(result => { this.simulating.set(false); this.simulation.set(result); });
    effect(() => {
      const type = this.fuel() === 'ALL' ? 'MOTOR' : this.fuel() as FuelType;
      const rows = this.rows().filter(row => row.prices[type] !== null);
      this.workspace.competitionBars.set(rows.map((row, index) => ({ label: row.name, color: row.own ? '#FF6B00' : index % 2 ? '#0284C7' : '#0B192C', points: [{ label: row.name, value: row.prices[type]! }] })));
      this.workspace.competitionLines.set(rows.map((row, index) => {
        const record = this.comparison()?.fuels.find(f => f.fuelType === type);
        const date = row.own ? record?.bellavistaValidFrom : record?.observations.find(o => o.competitorId === row.id)?.observedAt.slice(0, 10);
        return { label: row.name, color: row.own ? '#FF6B00' : index % 2 ? '#0284C7' : '#0B192C', points: date ? [{ label: date, value: row.prices[type]! }] : [] };
      }));
    });
  }
  reload(): void { this.reload$.next(); }
  open(kind: 'station' | 'observation'): void { this.formError.set(''); this.success.set(''); this.dialog.set(kind); }
  reference(type: FuelType): string | null { return this.comparison()?.fuels.find(f => f.fuelType === type)?.competition.average ?? null; }
  delta(row: MatrixRow, type: FuelType): string | null {
    const own = this.comparison()?.fuels.find(f => f.fuelType === type)?.bellavistaPrice ?? null, value = row.prices[type];
    return own === null || value === null ? null : difference(value, own);
  }
  priority(): FuelType { return this.fuel() === 'ALL' ? 'MOTOR' : this.fuel() as FuelType; }
  saveStation(): void {
    this.stationForm.markAllAsTouched(); this.formError.set('');
    if (this.stationForm.invalid || this.saving()) { this.formError.set('Completa nombre, bandera y ubicación.'); return; }
    this.saving.set(true);
    this.api.register(this.stationForm.getRawValue()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: station => {
      this.saving.set(false); this.dialog.set(null); this.stationForm.reset(); this.observationForm.controls.competitorId.setValue(station.id);
      this.success.set('Estación registrada correctamente.'); this.reload();
    }, error: (error: unknown) => { this.saving.set(false); applyFieldErrors(this.stationForm, error); this.formError.set(this.message(error)); } });
  }
  saveObservation(): void {
    this.observationForm.markAllAsTouched(); this.formError.set('');
    const values = this.observationForm.getRawValue();
    const validTime = /^\d{4}-\d{2}-\d{2}T(?:[01]\d|2[0-3]):[0-5]\d(?::[0-5]\d)?$/.test(values.observedAt) && validDate(values.observedAt.slice(0, 10));
    const date = Date.parse(values.observedAt + '-05:00');
    if (this.observationForm.invalid || !validTime || !Number.isFinite(date) || date > Date.now() || this.saving()) { this.formError.set('Selecciona una estación, un precio válido y una fecha de Colombia que no esté en el futuro.'); return; }
    this.saving.set(true);
    this.api.observe({ ...values, observedAt: new Date(date).toISOString() }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: () => {
      this.saving.set(false); this.dialog.set(null); this.observationForm.controls.pricePerGallon.reset(); this.success.set('Observación de precio registrada correctamente.'); this.reload();
    }, error: (error: unknown) => { this.saving.set(false); applyFieldErrors(this.observationForm, error); this.formError.set(this.message(error)); } });
  }
  simulate(): void {
    this.simulationForm.markAllAsTouched(); const values = this.simulationForm.getRawValue();
    if (this.simulationForm.invalid || !values[values.fuelType]) { this.simulationError.set('Ingresa un precio válido para el combustible seleccionado y revisa el costo opcional.'); return; }
    this.simulate$.next();
  }
  private message(error: unknown): string {
    return isApiError(error) && error.status === 404 ? 'El servicio de competencia no está disponible en este servidor. Las otras pestañas siguen disponibles.' : getErrorMessage(error);
  }
}
