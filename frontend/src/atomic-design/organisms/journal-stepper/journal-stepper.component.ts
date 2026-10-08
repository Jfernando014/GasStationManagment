import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { JournalStepResponse, StepStatus } from '../../../shared/models/journal.model';

@Component({
  selector: 'app-journal-stepper',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './journal-stepper.component.html',
  styleUrls: ['./journal-stepper.component.css']
})
export class JournalStepperComponent {
  @Input() steps: JournalStepResponse[] = [];

  getStepClass(status: StepStatus): string {
    switch (status) {
      case StepStatus.COMPLETED:
        return 'stepper-completed';
      case StepStatus.IN_PROGRESS:
        return 'stepper-active';
      case StepStatus.PENDING:
      default:
        return 'stepper-pending';
    }
  }

  getRouteForStep(stepNumber: number): string {
    // Definimos las rutas hijas para cada paso
    const routes: Record<number, string> = {
      1: 'step-1-medicion-inicial',
      2: 'step-2-asignacion-turnos',
      3: 'step-3-apertura-surtidores',
      4: 'step-4-registro-lecturas',
      5: 'step-5-cierre-surtidores',
      6: 'step-6-cuadre-caja'
    };
    return routes[stepNumber] || '';
  }
}
