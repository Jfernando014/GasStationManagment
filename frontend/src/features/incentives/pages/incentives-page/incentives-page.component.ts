import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { InfoTooltipComponent } from '../../../../atomic-design/atoms/info-tooltip/info-tooltip.component';
import { ToastComponent } from '../../../../atomic-design/molecules/toast/toast.component';

interface IncentiveTab {
  label: string;
  /** Child route of `resultados/incentivos`. */
  path: string;
  /** False while the route does not exist yet: the tab shows disabled. */
  enabled: boolean;
}

/** Container of Resultados › Incentivos: the tabs and the outlet of the selected one. */
@Component({
  selector: 'app-incentives-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, InfoTooltipComponent, ToastComponent],
  templateUrl: './incentives-page.component.html',
  styleUrl: './incentives-page.component.css'
})
export class IncentivesPageComponent {
  readonly unavailableHint = 'Disponible más adelante';
  readonly tabs: IncentiveTab[] = [
    { label: 'Metas de combustible', path: 'metas', enabled: false },
    { label: 'Aditivos', path: 'aditivos', enabled: true },
    { label: 'Liquidación del mes', path: 'liquidacion', enabled: false }
  ];
}
