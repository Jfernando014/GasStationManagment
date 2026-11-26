import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { AuthService } from '../../../../core/services/auth.service';
import { SalesParametersService } from '../../../../core/services/sales-parameters.service';
import { ToastService } from '../../../../core/services/toast.service';
import { valueOn } from '../../../../shared/helpers/dated-value.helpers';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { SALES_PARAMETERS } from '../../../../shared/helpers/sales-parameter.helpers';
import { stationToday } from '../../../../shared/helpers/shift-display.helpers';
import { SalesParameterDto, SalesParameterKey } from '../../../../shared/models/dtos/sales-parameter.dto';
import { SalesParameterHistoryComponent } from '../../components/sales-parameter-history/sales-parameter-history.component';
import { SalesParameterPanelComponent } from '../../components/sales-parameter-panel/sales-parameter-panel.component';

const KEY: SalesParameterKey = 'credibanco-commission-rate';
/** How long the row just saved stays highlighted. */
const HIGHLIGHT_MS = 2600;

/** Configuración › Parámetros › Comisión de tarjeta: the commission of Credibanco, its history and a new value. */
@Component({
  selector: 'app-commission-tab',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ActionButtonDirective, SkeletonComponent, TagComponent, EmptyStateComponent,
    SalesParameterHistoryComponent, SalesParameterPanelComponent
  ],
  templateUrl: './commission-tab.component.html',
  styleUrl: './commission-tab.component.css'
})
export class CommissionTabComponent {
  private readonly service = inject(SalesParametersService);
  private readonly toast = inject(ToastService);
  private highlightTimer: ReturnType<typeof setTimeout> | null = null;

  protected readonly key = KEY;
  protected readonly canEdit = inject(AuthService).isAdmin();
  protected readonly today = stationToday();
  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly values = signal<SalesParameterDto[]>([]);
  protected readonly panelOpen = signal(false);
  protected readonly highlight = signal<string | null>(null);

  protected readonly current = computed(() => {
    const value = valueOn(this.values(), this.today);
    return value
      ? { text: SALES_PARAMETERS[KEY].format(value.value), since: `Vigente desde el ${formatShortDate(value.validFrom)}` }
      : null;
  });

  constructor() {
    inject(DestroyRef).onDestroy(() => {
      if (this.highlightTimer) clearTimeout(this.highlightTimer);
      this.toast.hide();
    });
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.service.history(KEY).subscribe({
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
    this.toast.show(`Comisión Credibanco guardada. ${when} ${formatShortDate(saved.validFrom)}.`);
    this.highlight.set(saved.validFrom);
    if (this.highlightTimer) clearTimeout(this.highlightTimer);
    this.highlightTimer = setTimeout(() => this.highlight.set(null), HIGHLIGHT_MS);
  }
}
