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

  getStepClass(status: StepStatus, stepNumber: number): string {
    if (status === StepStatus.COMPLETED) {
      return 'stepper-completed';
    }
    // We assume the first PENDING or IN_PROGRESS is active.
    // For now, we will rely on routerLinkActive to highlight the active tab,
    // but we can add a base class.
    return 'stepper-pending';
  }

  getStepName(stepNumber: number): string {
    const names: Record<number, string> = {
      1: 'Personal',
      2: 'Vendedores',
      3: 'Electrónicos',
      4: 'Caja',
      5: 'Inventario',
      6: 'Cierre'
    };
    return names[stepNumber] || `Paso ${stepNumber}`;
  }

  getRouteForStep(stepNumber: number): string {
    const routes: Record<number, string> = {
      1: 'personal',
      2: 'vendedores',
      3: 'electronicos',
      4: 'caja',
      5: 'inventario',
      6: 'cierre'
    };
    return routes[stepNumber] || '';
  }
}
