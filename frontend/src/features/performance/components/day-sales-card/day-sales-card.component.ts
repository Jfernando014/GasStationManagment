import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AvatarComponent } from '../../../../atomic-design/atoms/avatar/avatar.component';
import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { SkeletonComponent } from '../../../../atomic-design/atoms/skeleton/skeleton.component';
import { WarningPanelComponent } from '../../../../atomic-design/molecules/warning-panel/warning-panel.component';
import { DaySaleView } from '../../models/performance-view.model';

/**
 * One day, one card: what each seller sold in the shift of that day. A day has no index, because every seller was the
 * only one in their shift; this card shows the sales themselves. A seller with a shift and no sales gets a warning that
 * points to the closing of that day, and the ones that rested are listed dimmed at the end.
 */
@Component({
  selector: 'app-day-sales-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AvatarComponent, ShiftChipComponent, SkeletonComponent, WarningPanelComponent, RouterLink],
  templateUrl: './day-sales-card.component.html',
  styleUrl: './day-sales-card.component.css'
})
export class DaySalesCardComponent {
  readonly rows = input.required<DaySaleView[]>();
  /** "Viernes 20 nov 2026". */
  readonly dayLabel = input.required<string>();
  /** Day as `YYYY-MM-DD`, for the link to its journal. */
  readonly date = input.required<string>();
  readonly total = input('');
  readonly loading = input(false);
  /** Changes with every new answer, so the bars grow again. */
  readonly version = input(0);

  protected readonly skeletonRows = [0, 1, 2];

  protected shiftName(row: DaySaleView): string {
    return row.period === 'NIGHT' ? 'turno de noche' : row.period === 'DAY' ? 'turno de día' : '';
  }
}
