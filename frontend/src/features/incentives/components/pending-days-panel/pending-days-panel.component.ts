import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, Injector, afterNextRender, computed, inject, input,
  output, signal
} from '@angular/core';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { WarningPanelComponent } from '../../../../atomic-design/molecules/warning-panel/warning-panel.component';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { shortDateLabel } from '../../../../shared/helpers/shift-display.helpers';
import { PendingDay } from '../../models/incentive-calculation.model';
import { AdditiveSalesService } from '../../services/additive-sales.service';

/** pending → asking (confirmation) → running → done, or failed (then it can be tried again). */
type DayStatus = 'pending' | 'asking' | 'running' | 'done' | 'failed';

interface DayState {
  status: DayStatus;
  /** Why the last attempt failed. */
  message: string;
}

interface DayRowView {
  date: string;
  /** "vie 22 ago 2026". */
  label: string;
  /** "22 ago". */
  short: string;
  status: DayStatus;
  message: string;
}

const PENDING_STATE: DayState = { status: 'pending', message: '' };

/**
 * Closed days whose additive sales are not stored yet, so no total includes them. The administrator can
 * reprocess each day after confirming it; anyone else only sees which days are pending.
 */
@Component({
  selector: 'app-pending-days-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ActionButtonDirective, TagComponent, WarningPanelComponent],
  templateUrl: './pending-days-panel.component.html',
  styleUrl: './pending-days-panel.component.css'
})
export class PendingDaysPanelComponent {
  private readonly service = inject(AdditiveSalesService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly destroyRef = inject(DestroyRef);

  readonly days = input.required<PendingDay[]>();
  /** Only the administrator exists today; with more roles this hides the reprocess buttons. */
  readonly canReprocess = input(true);
  /** A day was processed: the totals on screen are out of date. */
  readonly reprocessed = output<void>();

  private readonly states = signal<Record<string, DayState>>({});

  protected readonly rows = computed<DayRowView[]>(() =>
    this.days().map(day => {
      const state = this.states()[day.date] ?? PENDING_STATE;
      return {
        date: day.date,
        label: `${shortDateLabel(day.date).slice(0, 3)} ${formatShortDate(day.date)}`,
        short: formatShortDate(day.date, false),
        status: state.status,
        message: state.message
      };
    }));

  /** Days that still have to be processed; a day just processed stays in the list but no longer counts. */
  protected readonly pendingCount = computed(() => this.rows().filter(row => row.status !== 'done').length);

  /** Read-only view: "22 ago · 27 ago"; the year only when the days are in different years. */
  protected readonly datesLine = computed(() => {
    const days = this.days().map(day => day.date);
    const withYear = new Set(days.map(day => day.slice(0, 4))).size > 1;
    return days.map(day => formatShortDate(day, withYear)).join(' · ');
  });

  protected ask(date: string): void {
    this.setState(date, 'asking');
    this.focusIn(date, '.actions button');
  }

  protected cancel(date: string): void {
    this.setState(date, 'pending');
    this.focusIn(date, '.actions button');
  }

  protected reprocess(date: string): void {
    this.setState(date, 'running');
    this.focusIn(date, null);
    this.service.reprocess(date).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(result => {
      if (result.outcome === 'PROCESSED') {
        this.setState(date, 'done');
        this.reprocessed.emit();
      } else {
        this.setState(date, 'failed', result.message);
        this.focusIn(date, '.actions button');
      }
    });
  }

  private setState(date: string, status: DayStatus, message = ''): void {
    this.states.update(states => ({ ...states, [date]: { status, message } }));
  }

  /** Puts the focus on a control of the row once it is drawn, or on the row itself when there is no control. */
  private focusIn(date: string, selector: string | null): void {
    afterNextRender(() => {
      const row = this.host.nativeElement.querySelector<HTMLElement>(`[data-day="${date}"]`);
      (selector ? row?.querySelector<HTMLElement>(selector) : row)?.focus();
    }, { injector: this.injector });
  }
}
