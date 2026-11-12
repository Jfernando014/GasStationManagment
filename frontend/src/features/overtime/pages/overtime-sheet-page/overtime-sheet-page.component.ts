import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, Injector, afterNextRender, computed, effect, inject,
  input, signal, untracked, viewChild
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { ToastComponent } from '../../../../atomic-design/molecules/toast/toast.component';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { addDays, stationToday } from '../../../../shared/helpers/shift-display.helpers';
import { AdjustmentsPanelComponent } from '../../components/adjustments-panel/adjustments-panel.component';
import { OvertimeNoticeComponent } from '../../components/overtime-notice/overtime-notice.component';
import { OvertimeSheetTableComponent } from '../../components/overtime-sheet-table/overtime-sheet-table.component';
import {
  fortnightFromQuery, fortnightLabel, formatHours, negativeCategories
} from '../../helpers/overtime-display.helpers';
import { LoadResult, OvertimeSheet, OvertimeSummary } from '../../models/overtime.model';
import { OvertimeParametersService } from '../../services/overtime-parameters.service';
import { OvertimeSheetService } from '../../services/overtime-sheet.service';

/** A stretch of the fortnight calculated with one version of the parameters. */
interface VersionStretch {
  from: string;
  to: string;
  label: string;
}

/** Personal › Horas extra › one worker: the sheet of the fortnight day by day, with its adjustments. */
@Component({
  selector: 'app-overtime-sheet-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink, ActionButtonDirective, SkeletonComponent, TagComponent, EmptyStateComponent, ToastComponent,
    AdjustmentsPanelComponent, OvertimeNoticeComponent, OvertimeSheetTableComponent
  ],
  templateUrl: './overtime-sheet-page.component.html',
  styleUrl: './overtime-sheet-page.component.css'
})
export class OvertimeSheetPageComponent {
  private readonly sheets = inject(OvertimeSheetService);
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

  /** Previous and next worker of the fortnight and the position "2 de 4"; null when the worker is not in the summary. */
  protected readonly neighbours = computed(() => {
    const workers = this.summary()?.workers ?? [];
    const index = workers.findIndex(row => row.worker.id === this.workerId());
    if (index < 0) return null;
    return {
      position: `${index + 1} de ${workers.length}`,
      previous: index > 0 ? workers[index - 1].worker.id : null,
      next: index < workers.length - 1 ? workers[index + 1].worker.id : null
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

  /** Which version covered which days, when the fortnight used more than one. */
  protected readonly stretches = computed<VersionStretch[]>(() => {
    const sheet = this.sheet();
    if (!sheet || sheet.parameterVersions.length < 2) return [];
    const versions = sheet.parameterVersions;
    return versions.map((start, index) => ({
      from: index === 0 ? sheet.fortnight.from : start,
      to: index === versions.length - 1 ? sheet.fortnight.to : addDays(versions[index + 1], -1),
      label: this.versionNumbers()[start] ? `v${this.versionNumbers()[start]}` : `los parámetros del ${formatShortDate(start, false)}`
    }));
  });
  protected readonly changeText = computed(() => {
    const stretches = this.stretches();
    const day = (date: string) => formatShortDate(date, false);
    return stretches.map((stretch, index) => {
      if (index === 0) return `Del ${Number(stretch.from.slice(8, 10))} al ${day(stretch.to)} se calculó con ${stretch.label}`;
      if (index === stretches.length - 1) return `desde el ${day(stretch.from)}, con ${stretch.label}`;
      return `del ${Number(stretch.from.slice(8, 10))} al ${day(stretch.to)}, con ${stretch.label}`;
    }).join('; ') + '.';
  });
  /** First day of the newest version: "Ver qué cambió" opens it in the parameters. */
  protected readonly newestVersionFrom = computed(() => {
    const versions = this.sheet()?.parameterVersions ?? [];
    return versions[versions.length - 1] ?? '';
  });

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
