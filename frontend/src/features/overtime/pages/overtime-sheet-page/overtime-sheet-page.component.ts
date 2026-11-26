import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, Injector, afterNextRender, computed, effect, inject,
  input, signal, untracked, viewChild
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { startWith, Subscription, switchMap } from 'rxjs';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { PopoverComponent } from '../../../../atomic-design/molecules/popover/popover.component';
import { ToastComponent } from '../../../../atomic-design/molecules/toast/toast.component';
import { stationToday } from '../../../../shared/helpers/shift-display.helpers';
import { FortnightAccountComponent } from '../../components/fortnight-account/fortnight-account.component';
import { OvertimeGlossaryComponent } from '../../components/overtime-glossary/overtime-glossary.component';
import { OvertimeNoticeComponent } from '../../components/overtime-notice/overtime-notice.component';
import { OvertimeSheetTableComponent } from '../../components/overtime-sheet-table/overtime-sheet-table.component';
import {
  fortnightFromQuery, fortnightLabel, formatHours, negativeCategories
} from '../../helpers/overtime-display.helpers';
import { addCategoryHours, journalDays, lacksHours, roleLabel } from '../../helpers/overtime-view.helpers';
import { LoadResult, OvertimeSheet, OvertimeSummary } from '../../models/overtime.model';
import { JournalStatesService } from '../../services/journal-states.service';
import { OvertimeParametersService } from '../../services/overtime-parameters.service';
import { OvertimeSheetService } from '../../services/overtime-sheet.service';

/** Personal › Horas extra › one worker: the fortnight day by day and how its overtime adds up. */
@Component({
  selector: 'app-overtime-sheet-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink, ActionButtonDirective, SkeletonComponent, TagComponent, EmptyStateComponent, PopoverComponent,
    ToastComponent, FortnightAccountComponent, OvertimeGlossaryComponent, OvertimeNoticeComponent,
    OvertimeSheetTableComponent
  ],
  templateUrl: './overtime-sheet-page.component.html',
  styleUrl: './overtime-sheet-page.component.css'
})
export class OvertimeSheetPageComponent {
  private readonly sheets = inject(OvertimeSheetService);
  private readonly journalStates = inject(JournalStatesService);
  private readonly parameters = inject(OvertimeParametersService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);

  /** Only the administrator exists today; with more roles this hides the adjustments' buttons. */
  readonly canManage = input(true);

  private readonly params = toSignal(this.route.paramMap, { initialValue: this.route.snapshot.paramMap });
  private readonly query = toSignal(this.route.queryParamMap, { initialValue: this.route.snapshot.queryParamMap });
  private readonly period = computed(() => fortnightFromQuery(this.query(), stationToday()));

  protected readonly workerId = computed(() => this.params().get('workerId') ?? '');
  protected readonly month = computed(() => this.period().month);
  protected readonly fortnight = computed(() => this.period().number);
  protected readonly queryParams = computed(() => ({ mes: this.month(), quincena: this.fortnight() }));

  /** Answer for the sheet on screen; null while it loads. */
  private readonly result = signal<LoadResult<OvertimeSheet> | null>(null);
  /** Workers of the fortnight in order, to go to the previous and next ones. */
  private readonly summary = signal<OvertimeSummary | null>(null);
  /** Number of each version of the parameters by its first day. */
  protected readonly versionNumbers = signal<Record<string, number>>({});

  private readonly heading = viewChild<ElementRef<HTMLElement>>('heading');
  private loadSubscription: Subscription | null = null;

  protected readonly sheet = computed(() => {
    const result = this.result();
    return result?.outcome === 'OK' ? result.data : null;
  });
  protected readonly state = computed(() => {
    const result = this.result();
    return !result ? 'loading' : result.outcome === 'OK' ? 'ready' : result.outcome === 'NOT_FOUND' ? 'missing' : 'error';
  });
  protected readonly errorMessage = computed(() => {
    const result = this.result();
    return result && result.outcome === 'ERROR' ? result.message : '';
  });

  protected readonly periodText = computed(() => {
    const sheet = this.sheet();
    return sheet ? fortnightLabel(sheet.fortnight) : '';
  });

  protected readonly role = computed(() => roleLabel(this.sheet()?.worker.role ?? null));

  /** Closed journals of the month; null while they load or when they could not be read. */
  protected readonly closedDays = toSignal(toObservable(this.month).pipe(
    switchMap(month => this.journalStates.closedDays(month).pipe(startWith(null)))), { initialValue: null });

  /** Journals of past days that are still open, and whether any of them already has its hours. */
  protected readonly openDays = computed(() => {
    const sheet = this.sheet();
    if (!sheet) return { count: 0, withHours: false };
    const open = journalDays(sheet.fortnight.from, sheet.fortnight.to, sheet.affectedDates, stationToday(),
      this.closedDays()).filter(day => day.state === 'open');
    return { count: open.length, withHours: open.some(day => !lacksHours(day.reason)) };
  });

  /** Hours of the fortnight per category, all workers together, for the glossary. */
  protected readonly totals = computed(() => {
    const summary = this.summary();
    return summary ? addCategoryHours(summary.workers.map(row => row.totalToPay)) : null;
  });

  /** Previous and next worker of the fortnight and the position "2 de 4"; null when the worker is not in the summary. */
  protected readonly neighbours = computed(() => {
    const workers = this.summary()?.workers ?? [];
    const index = workers.findIndex(row => row.worker.id === this.workerId());
    if (index < 0) return null;
    return {
      position: `${index + 1} de ${workers.length}`,
      previous: index > 0 ? workers[index - 1].worker : null,
      next: index < workers.length - 1 ? workers[index + 1].worker : null
    };
  });

  /** Categories whose total to pay is below zero, written as the sentence of the alert. */
  protected readonly negativeText = computed(() => {
    const sheet = this.sheet();
    if (!sheet) return '';
    return negativeCategories(sheet.totalToPay).map(category => {
      const hours = sheet.totalToPay[category];
      return `${category} quedó en ${formatHours(hours)} ${Math.abs(hours) === 1 ? 'hora' : 'horas'}`;
    }).join(' y ');
  });
  protected readonly hasNegative = computed(() => this.negativeText() !== '');

  constructor() {
    effect(() => {
      const workerId = this.workerId();
      const month = this.month();
      const fortnight = this.fortnight();
      untracked(() => this.load(workerId, month, fortnight));
    });
  }

  protected retry(): void {
    this.load(this.workerId(), this.month(), this.fortnight());
  }

  /** An adjustment was added or removed: the sheet is asked again without clearing the screen. */
  protected refresh(): void {
    const workerId = this.workerId();
    this.sheets.getSheet(workerId, this.month(), this.fortnight()).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(result => {
        if (result.outcome === 'OK' && this.workerId() === workerId) this.result.set(result);
      });
  }

  private load(workerId: string, month: string, fortnight: number): void {
    this.loadSubscription?.unsubscribe();
    this.result.set(null);
    this.loadSubscription = this.sheets.getSheet(workerId, month, fortnight).pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(result => {
        this.result.set(result);
        // The keyboard and screen readers start again at the title of the new page.
        afterNextRender(() => this.heading()?.nativeElement.focus({ preventScroll: true }), { injector: this.injector });
        if (result.outcome === 'OK' && result.data.parameterVersions.length > 1) this.loadVersionNumbers();
      });
    this.sheets.getSummary(month, fortnight).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(result => {
      if (result.outcome === 'OK') this.summary.set(result.data);
    });
  }

  /** The numbers of the versions are only needed when a fortnight used more than one. */
  private loadVersionNumbers(): void {
    this.parameters.getHistory().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(result => {
      if (result.outcome !== 'OK') return;
      this.versionNumbers.set(Object.fromEntries(result.data.map(version => [version.validFrom, version.number])));
    });
  }
}
