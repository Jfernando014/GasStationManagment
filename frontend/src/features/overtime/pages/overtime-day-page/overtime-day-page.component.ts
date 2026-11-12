import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, Injector, afterNextRender, computed, effect, inject,
  input, signal, untracked, viewChild
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { ShiftsScheduleService } from '../../../../core/services/shifts-schedule.service';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { capitalize, longDateLabel, stationToday } from '../../../../shared/helpers/shift-display.helpers';
import { fortnightFromQuery, fortnightOf } from '../../helpers/overtime-display.helpers';
import { DayHours, LoadResult } from '../../models/overtime.model';
import { WorkerHoursService } from '../../services/worker-hours.service';

const CALENDAR_DAY = /^\d{4}-\d{2}-\d{2}$/;

/** What the page tells about the day: its hours were never recorded, are pending, or were saved. */
type DayState = 'loading' | 'missing-link' | 'error' | 'not-registered' | 'pending' | 'unconfirmed' | 'processed';

/** One line of "Quién trabajó". */
interface WorkerLine {
  key: string;
  workerId: string | null;
  name: string;
  code: string | null;
  hours: number | null;
  rest: boolean;
  novelty: string | null;
}

type ReprocessState = 'idle' | 'asking' | 'running' | 'failed';

/** Personal › Horas extra › one day: what happened to its hours and who worked, with the reprocess for the admin. */
@Component({
  selector: 'app-overtime-day-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink, ActionButtonDirective, ShiftChipComponent, SkeletonComponent, TagComponent, EmptyStateComponent
  ],
  templateUrl: './overtime-day-page.component.html',
  styleUrl: './overtime-day-page.component.css'
})
export class OvertimeDayPageComponent {
  private readonly hoursService = inject(WorkerHoursService);
  private readonly schedule = inject(ShiftsScheduleService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);

  /** Only the administrator exists today; with more roles this hides the reprocess button. */
  readonly canManage = input(true);

  private readonly params = toSignal(this.route.paramMap, { initialValue: this.route.snapshot.paramMap });
  private readonly query = toSignal(this.route.queryParamMap, { initialValue: this.route.snapshot.queryParamMap });

  protected readonly date = computed(() => this.params().get('date') ?? '');
  private readonly validDate = computed(() => CALENDAR_DAY.test(this.date()) && !Number.isNaN(Date.parse(this.date())));

  /** Fortnight to go back to: the one in the address, or else the one that contains the day. */
  protected readonly queryParams = computed(() => {
    const query = this.query();
    const period = !query.has('mes') && this.validDate()
      ? fortnightOf(this.date()) : fortnightFromQuery(query, stationToday());
    return { mes: period.month, quincena: period.number };
  });

  protected readonly title = computed(() => {
    const date = this.date();
    if (!this.validDate()) return 'Día';
    return `${capitalize(longDateLabel(date).split(' ')[0])} ${formatShortDate(date)}`;
  });
  protected readonly shortDay = computed(() => (this.validDate() ? formatShortDate(this.date(), false) : ''));

  /** Answer for the day on screen; null while it loads. */
  private readonly result = signal<LoadResult<DayHours> | null>(null);
  /** Who the grid had on the day, for the days without saved hours. */
  private readonly gridLines = signal<WorkerLine[] | null>(null);
  protected readonly gridFailed = signal(false);

  protected readonly reprocessState = signal<ReprocessState>('idle');
  protected readonly reprocessMessage = signal('');
  /** The reprocess worked: the day is shown as processed with this note. */
  protected readonly justProcessed = signal(false);

  private readonly heading = viewChild<ElementRef<HTMLElement>>('heading');
  private readonly page = viewChild<ElementRef<HTMLElement>>('page');
  private loadSubscription: Subscription | null = null;

  protected readonly day = computed(() => {
    const result = this.result();
    return result?.outcome === 'OK' ? result.data : null;
  });

  protected readonly state = computed<DayState>(() => {
    if (!this.validDate()) return 'missing-link';
    const result = this.result();
    if (!result) return 'loading';
    if (result.outcome === 'NOT_FOUND') return 'not-registered';
    if (result.outcome === 'ERROR') return 'error';
    if (result.data.status === 'PENDING') return 'pending';
    return result.data.noveltiesApplied ? 'processed' : 'unconfirmed';
  });
  protected readonly errorMessage = computed(() => {
    const result = this.result();
    return result && result.outcome === 'ERROR' ? result.message : '';
  });

  /** Saved hours when there are some, the grid otherwise. */
  protected readonly usesGrid = computed(() => ['pending', 'not-registered'].includes(this.state()));

  /** The grid of the day is still being read. */
  protected readonly gridLoading = computed(() => this.usesGrid() && this.gridLines() === null && !this.gridFailed());

  protected readonly lines = computed<WorkerLine[]>(() => {
    if (this.usesGrid()) return this.gridLines() ?? [];
    return (this.day()?.workers ?? []).map(worker => ({
      key: worker.workerId,
      workerId: worker.workerId,
      name: worker.name,
      code: worker.workedCode ?? worker.gridCode,
      hours: worker.hours,
      rest: worker.workedCode === null,
      novelty: worker.novelty
    }));
  });

  /** The administrator reprocesses a day whose hours are pending or were saved without the novelties. */
  protected readonly canReprocess = computed(() =>
    this.canManage() && ['pending', 'unconfirmed'].includes(this.state()));

  constructor() {
    effect(() => {
      const date = this.date();
      if (!this.validDate()) return;
      untracked(() => this.load(date));
    });
  }

  protected retry(): void {
    this.load(this.date());
  }

  protected ask(): void {
    this.reprocessMessage.set('');
    this.reprocessState.set('asking');
    afterNextRender(() => this.page()?.nativeElement.querySelector<HTMLElement>('.ask button')?.focus(), { injector: this.injector });
  }

  protected cancel(): void {
    this.reprocessState.set('idle');
    afterNextRender(() => this.page()?.nativeElement.querySelector<HTMLElement>('.reprocess-button')?.focus(), { injector: this.injector });
  }

  protected reprocess(): void {
    const date = this.date();
    this.reprocessState.set('running');
    this.hoursService.reprocess(date).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(result => {
      if (result.outcome === 'PROCESSED') {
        this.reprocessState.set('idle');
        this.justProcessed.set(true);
        this.refresh(date);
      } else {
        this.reprocessMessage.set(result.message);
        this.reprocessState.set('failed');
        afterNextRender(() => this.page()?.nativeElement.querySelector<HTMLElement>('.failed button')?.focus(), { injector: this.injector });
      }
    });
  }

  /** The day is asked again without clearing the screen. */
  private refresh(date: string): void {
    this.hoursService.getDay(date).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(result => {
      if (this.date() === date && result.outcome === 'OK') this.result.set(result);
    });
  }

  private load(date: string): void {
    this.loadSubscription?.unsubscribe();
    this.result.set(null);
    this.gridLines.set(null);
    this.gridFailed.set(false);
    this.reprocessState.set('idle');
    this.justProcessed.set(false);
    this.loadSubscription = this.hoursService.getDay(date).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(result => {
      this.result.set(result);
      // The keyboard and screen readers start again at the title of the new page.
      afterNextRender(() => this.heading()?.nativeElement.focus({ preventScroll: true }), { injector: this.injector });
      const withoutHours = result.outcome === 'NOT_FOUND' || (result.outcome === 'OK' && result.data.status === 'PENDING');
      if (withoutHours) this.loadGrid(date);
    });
  }

  /** The grid of Turnos for that one day: who was meant to work, without novelties. */
  private loadGrid(date: string): void {
    this.schedule.getSchedule(date, date).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: response => {
        const lines = response.workers.map<WorkerLine>(worker => {
          const code = worker.days.find(item => item.date === date)?.shiftCode ?? null;
          return {
            key: worker.workerId, workerId: worker.workerId, name: worker.fullName, code, hours: null,
            rest: code === null || code === 'DESCANSO', novelty: null
          };
        });
        this.gridLines.set(lines.sort((a, b) => Number(a.rest) - Number(b.rest) || a.name.localeCompare(b.name, 'es')));
      },
      error: () => this.gridFailed.set(true)
    });
  }
}
