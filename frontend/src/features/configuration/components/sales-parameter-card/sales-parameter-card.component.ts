import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, output, signal } from '@angular/core';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { InfoTooltipComponent } from '../../../../atomic-design/atoms/info-tooltip/info-tooltip.component';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { SidePanelComponent } from '../../../../atomic-design/organisms/side-panel/side-panel.component';
import { SalesParametersService } from '../../../../core/services/sales-parameters.service';
import { ToastService } from '../../../../core/services/toast.service';
import { newestFirst, valueOn } from '../../../../shared/helpers/dated-value.helpers';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { SALES_PARAMETERS } from '../../../../shared/helpers/sales-parameter.helpers';
import { stationToday } from '../../../../shared/helpers/shift-display.helpers';
import { SalesParameterDto, SalesParameterKey } from '../../../../shared/models/dtos/sales-parameter.dto';
import { SalesParameterHistoryComponent } from '../sales-parameter-history/sales-parameter-history.component';
import { SalesParameterPanelComponent } from '../sales-parameter-panel/sales-parameter-panel.component';

/**
 * A sales parameter in short: the value that rules on a day (today unless `asOf` says another), since when, the one
 * before, its history and the button to set a new one. When today's value is a different one, it says so too. It
 * reads its own history.
 */
@Component({
  selector: 'app-sales-parameter-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ActionButtonDirective, InfoTooltipComponent, SkeletonComponent, SidePanelComponent,
    SalesParameterHistoryComponent, SalesParameterPanelComponent
  ],
  templateUrl: './sales-parameter-card.component.html',
  styleUrl: './sales-parameter-card.component.css'
})
export class SalesParameterCardComponent implements OnInit {
  private readonly service = inject(SalesParametersService);
  private readonly toast = inject(ToastService);

  readonly parameterKey = input.required<SalesParameterKey>();
  /** False for who may only look: the button stays, disabled. */
  readonly canEdit = input(false);
  /** What follows "Desde …", e.g. "los 4 combustibles". */
  readonly scope = input('');
  /** Explanation behind a "?" next to the title. */
  readonly help = input('');
  /** Day ("YYYY-MM-DD") whose value is shown; today when null. */
  readonly asOf = input<string | null>(null);
  /** A value was saved. */
  readonly changed = output<void>();

  protected readonly today = stationToday();
  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly values = signal<SalesParameterDto[]>([]);
  protected readonly panelOpen = signal(false);
  protected readonly historyOpen = signal(false);

  protected readonly meta = computed(() => SALES_PARAMETERS[this.parameterKey()]);

  protected readonly summary = computed(() => {
    const sorted = newestFirst(this.values());
    const day = this.asOf() ?? this.today;
    const index = sorted.findIndex(value => value.validFrom <= day);
    const shown = index < 0 ? null : sorted[index];
    const previous = index < 0 ? null : sorted[index + 1] ?? null;
    const current = valueOn(sorted, this.today);
    const next = sorted.filter(value => value.validFrom > this.today).at(-1) ?? null;
    const { format } = this.meta();
    return {
      value: shown ? format(shown.value) : 'Sin valor',
      missing: shown === null,
      since: shown ? `Desde ${formatShortDate(shown.validFrom)}` : '',
      previous: previous ? `Antes ${format(previous.value)}` : '',
      today: current && current.value !== shown?.value
        ? `Hoy rige ${format(current.value)} desde ${formatShortDate(current.validFrom)}`
        : '',
      next: next ? `Programado: ${format(next.value)} desde el ${formatShortDate(next.validFrom)}` : ''
    };
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.service.history(this.parameterKey()).subscribe({
      next: values => {
        this.values.set(values);
        this.state.set('ready');
      },
      error: () => this.state.set('error')
    });
  }

  protected onSaved(saved: SalesParameterDto): void {
    this.values.update(values => [...values.filter(value => value.validFrom !== saved.validFrom), saved]);
    this.panelOpen.set(false);
    const when = saved.validFrom > this.today ? 'Empieza a regir el' : 'Rige desde el';
    this.toast.show(`${this.meta().title} guardado. ${when} ${formatShortDate(saved.validFrom)}.`);
    this.changed.emit();
  }
}
