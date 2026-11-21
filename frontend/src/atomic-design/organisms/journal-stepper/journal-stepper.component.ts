import { AfterViewInit, Component, DestroyRef, ElementRef, Input, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterModule } from '@angular/router';
import { filter } from 'rxjs';
import { JournalStepResponse, StepStatus } from '../../../shared/models/journal.model';

@Component({
  selector: 'app-journal-stepper',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './journal-stepper.component.html',
  styleUrls: ['./journal-stepper.component.css']
})
export class JournalStepperComponent implements AfterViewInit {
  @Input() steps: JournalStepResponse[] = [];

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  constructor() {
    inject(Router).events
      .pipe(filter(event => event instanceof NavigationEnd), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(() => this.revealActiveStep());
  }

  ngAfterViewInit(): void {
    this.revealActiveStep();
  }

  /** Con la barra deslizable (teléfono) el paso activo se centra; si todo cabe no hace nada. */
  private revealActiveStep(): void {
    requestAnimationFrame(() => {
      const container = this.host.nativeElement.querySelector<HTMLElement>('.stepper-container');
      if (!container || container.scrollWidth <= container.clientWidth) return;
      container.querySelector<HTMLElement>('.active-route')?.scrollIntoView?.({ inline: 'center', block: 'nearest' });
    });
  }

  get orderedSteps(): JournalStepResponse[] {
    return [...this.steps].sort((a, b) => a.stepNumber - b.stepNumber);
  }

  getStepClass(status: StepStatus): string {
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
