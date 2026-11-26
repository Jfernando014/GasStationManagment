import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { InfoTooltipComponent } from '../../../../atomic-design/atoms/info-tooltip/info-tooltip.component';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { DotValueListComponent } from '../../../../atomic-design/molecules/dot-value-list/dot-value-list.component';
import { CATEGORY_HELP, EXTRA_CATEGORIES, SURCHARGE_CATEGORIES } from '../../helpers/overtime-display.helpers';
import { hoursShort, sumHours, surchargeItems } from '../../helpers/overtime-view.helpers';
import { OvertimeSheet } from '../../models/overtime.model';
import { AdjustmentsPanelComponent } from '../adjustments-panel/adjustments-panel.component';

/** How the overtime to pay of one worker adds up in a fortnight, with the manual adjustments in their place. */
@Component({
  selector: 'app-fortnight-account',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [InfoTooltipComponent, TagComponent, DotValueListComponent, AdjustmentsPanelComponent],
  templateUrl: './fortnight-account.component.html',
  styleUrl: './fortnight-account.component.css'
})
export class FortnightAccountComponent {
  readonly sheet = input.required<OvertimeSheet>();
  readonly canManage = input(true);
  /** Hours are still missing from the fortnight. */
  readonly partial = input(false);
  /** An adjustment was added or removed: the sheet is out of date. */
  readonly changed = output<void>();

  protected readonly worked = computed(() => hoursShort(this.sheet().grossHours));

  protected readonly surcharges = computed(() => {
    const sheet = this.sheet();
    const items = surchargeItems(sheet.totalToPay);
    const total = sumHours(sheet.totalToPay, SURCHARGE_CATEGORIES);
    return {
      items,
      total: items.length ? hoursShort(total) : 'Ninguno',
      negative: total < 0,
      adjusted: SURCHARGE_CATEGORIES.some(category => sheet.adjustmentsNet[category] !== 0)
    };
  });

  protected readonly dayExtras = computed(() => hoursShort(sumHours(this.sheet().subtotals, EXTRA_CATEGORIES)));

  /** The fixed line, only for the roles that have it. */
  protected readonly fixed = computed(() => {
    const { fixedHedo, worker } = this.sheet();
    if (!fixedHedo.applies) return null;
    const role = worker.role?.toLowerCase() ?? 'trabajador';
    return {
      label: `Fijas de ${role}`,
      hours: hoursShort(fixedHedo.hours),
      help: `Cada ${role} recibe ${fixedHedo.hours} horas extra diurnas por quincena, trabaje o no horas de más.`
    };
  });

  protected readonly toPay = computed(() => {
    const hours = this.sheet().totalToPay;
    const total = sumHours(hours, EXTRA_CATEGORIES);
    return {
      total: hoursShort(total),
      negative: total < 0,
      parts: EXTRA_CATEGORIES.filter(category => hours[category] !== 0).map(category => ({
        category,
        text: `${hoursShort(hours[category])} ${CATEGORY_HELP[category].title.toLowerCase()} (${category})`,
        negative: hours[category] < 0
      }))
    };
  });
}
