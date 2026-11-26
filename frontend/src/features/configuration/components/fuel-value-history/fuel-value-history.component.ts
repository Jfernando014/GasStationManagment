import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';
import { EmptyStateComponent } from '../../../../atomic-design/molecules/empty-state/empty-state.component';
import { formatShortDate } from '../../../../shared/helpers/format.helpers';
import { DATED_VALUE_STATUS, newestFirst, valueOn, valueStatus } from '../../../../shared/helpers/dated-value.helpers';
import { formatFuelValue } from '../../../../shared/helpers/fuel-price.helpers';
import { FuelValueDto, FuelValueKind } from '../../../../shared/models/dtos/fuel-price.dto';

/** History of one kind of value of one fuel, newest first, each one with its state against today. */
@Component({
  selector: 'app-fuel-value-history',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ActionButtonDirective, TagComponent, EmptyStateComponent],
  templateUrl: './fuel-value-history.component.html',
  styleUrl: './fuel-value-history.component.css'
})
export class FuelValueHistoryComponent {
  readonly heading = input.required<string>();
  readonly kind = input.required<FuelValueKind>();
  /** Values of a single fuel, in any order. */
  readonly values = input.required<FuelValueDto[]>();
  readonly today = input.required<string>();
  /** Row just saved, highlighted for a moment. */
  readonly highlightId = input<number | null>(null);
  readonly emptyHeading = input.required<string>();
  readonly emptyText = input.required<string>();
  readonly edit = output<FuelValueDto>();
  readonly add = output<void>();

  protected readonly rows = computed(() => {
    const current = valueOn(this.values(), this.today());
    return newestFirst(this.values()).map(value => ({
      source: value,
      date: formatShortDate(value.validFrom),
      amount: formatFuelValue(this.kind(), value.pricePerGallon),
      ...DATED_VALUE_STATUS[valueStatus(value, current, this.today())]
    }));
  });
}
