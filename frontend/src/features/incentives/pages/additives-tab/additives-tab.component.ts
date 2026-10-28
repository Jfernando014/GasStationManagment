import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, Injector, afterNextRender, computed, effect, inject,
  input, signal, untracked, viewChild
} from '@angular/core';

import { InfoTooltipComponent } from '../../../../atomic-design/atoms/info-tooltip/info-tooltip.component';
import { StatusStepComponent } from '../../../../atomic-design/atoms/status-step/status-step.component';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { formatCop, formatInputDate, formatShortDate } from '../../../../shared/helpers/format.helpers';
import { prefersReducedMotion, stationToday } from '../../../../shared/helpers/shift-display.helpers';
import { NewRulePanelComponent } from '../../components/new-rule-panel/new-rule-panel.component';
import { IncentiveRuleStatus } from '../../models/additive-incentive.model';
import { AdditiveIncentivesService } from '../../services/additive-incentives.service';

interface AdditiveRowView {
  id: number;
  name: string;
  /** "$1.200", "Sin incentivo" or "—". */
  value: string;
  since: string;
  next: string;
}

interface HistoryLineView {
  key: string;
  label: string;
  tone: 'upcoming' | 'active' | 'past';
  value: string;
  since: string;
  reason: string | null;
  previous: boolean;
}

type HistoryState =
  | { status: 'loading' }
  | { status: 'error' }
  | { status: 'ready'; lines: HistoryLineView[] };

const STATUS_VIEW: Record<IncentiveRuleStatus, { label: string; tone: HistoryLineView['tone'] }> = {
  SCHEDULED: { label: 'Programado', tone: 'upcoming' },
  CURRENT: { label: 'Vigente', tone: 'active' },
  PREVIOUS: { label: 'Anterior', tone: 'past' }
};

const HIGHLIGHT_COLOR = '#FFF2E8';
const HIGHLIGHT_MS = 1600;

/** "Aditivos" tab: what the seller earns per unit of each additive, with the history of each one. */
@Component({
  selector: 'app-additives-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TagComponent, StatusStepComponent, InfoTooltipComponent, NewRulePanelComponent],
  templateUrl: './additives-tab.component.html',
  styleUrl: './additives-tab.component.css'
})
export class AdditivesTabComponent {
  private readonly service = inject(AdditiveIncentivesService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly destroyRef = inject(DestroyRef);

  private readonly injector = inject(Injector);

  /** Only the administrator exists today; with more roles this hides the "Nueva regla" button. */
  readonly canManageRules = input(true);

  protected readonly date = this.service.date;
  protected readonly loading = this.service.loading;
  protected readonly loadError = this.service.loadError;
  protected readonly isToday = computed(() => this.date() === stationToday());
  protected readonly dateLabel = computed(() => formatInputDate(this.date()));
  protected readonly shortDate = computed(() => formatShortDate(this.date()));
  protected readonly skeletonRows = [0, 1, 2, 3, 4];
  protected readonly skeletonLines = [0, 1];

  /** Id of the additive whose history is open; only one at a time. */
  protected readonly openId = signal<number | null>(null);
  protected readonly histories = signal<Record<number, HistoryState>>({});

  protected readonly rows = computed<AdditiveRowView[]>(() =>
    this.service.additives()
      .map(row => {
        const hasCurrent = row.currentValue !== null;
        return {
          id: row.additiveId,
          name: row.additiveName,
          value: hasCurrent ? formatCop(row.currentValue ?? 0) : row.hasEverHadIncentive ? '—' : 'Sin incentivo',
          since: hasCurrent && row.currentSince ? formatShortDate(row.currentSince) : '',
          next: row.nextValue !== null && row.nextSince
            ? `${formatCop(row.nextValue)} el ${formatShortDate(row.nextSince)}` : ''
        };
      }));

  protected readonly panelOpen = signal(false);
  protected readonly panelAdditiveId = signal<number | null>(null);
  private readonly newRuleButton = viewChild<ElementRef<HTMLButtonElement>>('newRuleButton');
  /** Additive of a rule just created: it is highlighted when the reloaded list arrives. */
  private pendingHighlight: number | null = null;
  private lastLoadedDate: string | null = null;

  constructor() {
    this.service.load(stationToday());

    // A reload of the same day (a rule was created) makes the cached histories stale; a change of day does not.
    effect(() => {
      this.service.additives();
      untracked(() => {
        const date = this.service.date();
        if (this.lastLoadedDate === date) {
          this.histories.set({});
          const open = this.openId();
          if (open !== null) this.loadHistory(open);
        }
        this.lastLoadedDate = date;
        const highlight = this.pendingHighlight;
        if (highlight !== null) {
          this.pendingHighlight = null;
          afterNextRender(() => this.highlightRow(highlight), { injector: this.injector });
        }
      });
    });
  }

  protected toggle(id: number): void {
    if (this.openId() === id) {
      this.openId.set(null);
      return;
    }
    this.openId.set(id);
    if (!this.histories()[id] || this.histories()[id].status === 'error') this.loadHistory(id);
  }

  protected loadHistory(id: number): void {
    this.histories.update(all => ({ ...all, [id]: { status: 'loading' } }));
    this.service.getHistory(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: items => {
        const lines = items
          .map<HistoryLineView>(item => ({
            key: `${item.effectiveFrom}-${item.value}`,
            ...STATUS_VIEW[item.status],
            value: formatCop(item.value),
            since: `desde ${formatShortDate(item.effectiveFrom)}`,
            reason: item.reason,
            previous: item.status === 'PREVIOUS'
          }));
        this.histories.update(all => ({ ...all, [id]: { status: 'ready', lines } }));
      },
      error: () => this.histories.update(all => ({ ...all, [id]: { status: 'error' } }))
    });
  }

  protected onDateChange(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    if (/^\d{4}-\d{2}-\d{2}$/.test(value) && value !== this.date()) this.service.load(value);
  }

  protected openPicker(input: HTMLInputElement): void {
    try {
      input.showPicker();
    } catch {
      input.focus();
    }
  }

  protected backToToday(): void {
    this.service.load(stationToday());
  }

  /** Opens the panel; a rule is about to be created, so the list goes back to today's values first. */
  protected openPanel(): void {
    if (!this.isToday()) this.backToToday();
    this.panelAdditiveId.set(this.openId());
    this.panelOpen.set(true);
  }

  protected onRuleCreated(additiveId: number): void {
    this.pendingHighlight = additiveId;
    this.service.refresh();
  }

  protected onPanelClosed(): void {
    this.panelOpen.set(false);
    this.newRuleButton()?.nativeElement.focus();
  }

  protected reload(): void {
    this.service.refresh();
  }

  /** Paints the row of an additive and fades it to white; scrolls to it when it is out of sight. */
  highlightRow(additiveId: number): void {
    const row = this.host.nativeElement.querySelector<HTMLElement>(`[data-additive-id="${additiveId}"]`);
    if (!row) return;
    const reduced = prefersReducedMotion();
    row.scrollIntoView({ block: 'nearest', behavior: reduced ? 'auto' : 'smooth' });
    // Reduced motion keeps the color still for the same time instead of fading it.
    row.animate(
      [{ backgroundColor: HIGHLIGHT_COLOR }, { backgroundColor: reduced ? HIGHLIGHT_COLOR : '#FFFFFF' }],
      { duration: HIGHLIGHT_MS, easing: 'ease-out' }
    );
  }
}
