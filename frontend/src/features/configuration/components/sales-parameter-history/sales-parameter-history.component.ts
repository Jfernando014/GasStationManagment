import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { DATED_VALUE_STATUS, newestFirst, valueOn, valueStatus } from '../../../../shared/helpers/dated-value.helpers';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { SALES_PARAMETERS } from '../../../../shared/helpers/sales-parameter.helpers';
import { SalesParameterDto, SalesParameterKey } from '../../../../shared/models/dtos/sales-parameter.dto';

/** Every value of a sales parameter, newest first, each one with its state against today. */
@Component({
  selector: 'app-sales-parameter-history',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TagComponent],
  template: `
    <div class="line head" aria-hidden="true"><span>Rige desde</span><span class="amount">Valor</span></div>
    <ul>
      @for (row of rows(); track row.validFrom) {
        <li class="line" [class.is-new]="row.validFrom === highlight()" [style.--i]="$index">
          <span>{{ row.date }}</span>
          <b class="amount">{{ row.amount }}</b>
          <app-tag [variant]="row.tone">{{ row.label }}</app-tag>
        </li>
      } @empty {
        <li class="empty">Todavía no hay ningún valor cargado.</li>
      }
    </ul>
  `,
  styleUrl: './sales-parameter-history.component.css'
})
export class SalesParameterHistoryComponent {
  readonly parameterKey = input.required<SalesParameterKey>();
  readonly values = input.required<SalesParameterDto[]>();
  readonly today = input.required<string>();
  /** Start date of the value just saved, highlighted for a moment. */
  readonly highlight = input<string | null>(null);

  protected readonly rows = computed(() => {
    const current = valueOn(this.values(), this.today());
    const meta = SALES_PARAMETERS[this.parameterKey()];
    return newestFirst(this.values()).map(value => ({
      validFrom: value.validFrom,
      date: formatShortDate(value.validFrom),
      amount: meta.format(value.value),
      ...DATED_VALUE_STATUS[valueStatus(value, current, this.today())]
    }));
  });
}
