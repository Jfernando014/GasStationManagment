import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { ToastComponent } from '../../../../atomic-design/molecules/toast/toast.component';

interface ParameterTab {
  label: string;
  /** Child route of `configuracion/parametros`. */
  path: string;
}

/** Container of Configuración › Parámetros: the tabs of each group of parameters and the outlet of the selected one. */
@Component({
  selector: 'app-parameters-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, RouterOutlet, ToastComponent],
  templateUrl: './parameters-page.component.html',
  styleUrl: './parameters-page.component.css'
})
export class ParametersPageComponent {
  readonly tabs: ParameterTab[] = [
    { label: 'Horas extra', path: 'horas-extra' }
  ];
}
