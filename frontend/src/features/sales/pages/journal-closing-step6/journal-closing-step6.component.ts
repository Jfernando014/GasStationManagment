import { Component, OnInit, inject, ChangeDetectorRef, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { JournalService } from '../../../../core/services/journal.service';
import {
  AutonomyAlertSummaryItemDto,
  FuelVariationSummaryItemDto,
  JournalClosingSummaryResponse,
  JournalResponse,
  JournalStatus,
  StepStatus,
  StepSummaryItemDto
} from '../../../../shared/models/journal.model';
import { DailyDownloadsComponent } from '../../../../atomic-design/organisms/daily-downloads/daily-downloads.component';

@Component({
  selector: 'app-journal-closing-step6',
  standalone: true,
  imports: [CommonModule, RouterModule, DailyDownloadsComponent],
  templateUrl: './journal-closing-step6.component.html',
  styleUrls: ['./journal-closing-step6.component.css']
})
export class JournalClosingStep6Component implements OnInit {
  private readonly journalService = inject(JournalService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  journal: JournalResponse | null = null;
  summary: JournalClosingSummaryResponse | null = null;
  isLoading = false;
  isClosing = false;
  isConfirmDialogOpen = false;
  errorMessage = '';

  ngOnInit(): void {
    this.journalService.currentJournal$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(journal => {
        if (!journal) return;
        this.journal = journal;
        this.loadClosingSummary(journal);
      });
  }

  loadClosingSummary(journal: JournalResponse): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.journalService.getClosingSummary(journal.id).subscribe({
      next: (res) => {
        this.summary = res;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.summary = this.buildFallbackSummary(journal);
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private buildFallbackSummary(journal: JournalResponse): JournalClosingSummaryResponse {
    const stepLabels: Record<number, string> = {
      1: 'Personal del día',
      2: 'Cierre por vendedor',
      3: 'Medios electrónicos',
      4: 'Consolidado y caja',
      5: 'Tanques e inventario'
    };
    const steps: StepSummaryItemDto[] = [1, 2, 3, 4, 5].map(num => {
      const found = journal.steps.find(s => s.stepNumber === num);
      const status = found?.status ?? StepStatus.PENDING;
      return {
        stepNumber: num,
        stepName: stepLabels[num],
        status,
        hasDiscrepancy: false,
        summaryDetail: status === StepStatus.COMPLETED ? 'Paso completado' : 'Pendiente por completar'
      };
    });
    const completedCount = steps.filter(s => s.status === StepStatus.COMPLETED).length;
    return {
      journalId: journal.id,
      journalDate: journal.date,
      consecutive: journal.consecutive,
      status: journal.status,
      closedAt: journal.closedAt ?? null,
      completedStepsCount: completedCount,
      vendorsWithShortageCount: 0,
      vendorsWithSurplusCount: 0,
      newAlertsCount: 0,
      steps,
      fuelVariations: [],
      newAutonomyAlerts: []
    };
  }

  journalDate(): string {
    return this.summary?.journalDate || this.journal?.date || '';
  }

  isClosed(): boolean {
    return this.journal?.status === JournalStatus.CLOSED || this.summary?.status === JournalStatus.CLOSED;
  }

  closedTimeFormatted(): string {
    const raw = this.summary?.closedAt || this.journal?.closedAt;
    if (!raw) return '';
    const date = new Date(raw);
    if (isNaN(date.getTime())) return '';
    return date.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
  }

  shortDayMonth(): string {
    const date = this.journalDate();
    if (!date || !date.includes('-')) return '';
    const [, m, d] = date.split('-');
    return `${d}/${m}`;
  }

  discrepancyIndicatorText(): string {
    if (!this.summary) return '—';
    const nFalta = this.summary.vendorsWithShortageCount;
    const nSobra = this.summary.vendorsWithSurplusCount;
    if (nFalta > 0) {
      return `${nFalta} ${nFalta === 1 ? 'falta' : 'faltan'}${nSobra > 0 ? ` · ${nSobra} ${nSobra === 1 ? 'sobra' : 'sobran'}` : ''}`;
    }
    if (nSobra > 0) {
      return `${nSobra} ${nSobra === 1 ? 'sobra' : 'sobran'}`;
    }
    return 'Ninguno';
  }

  discrepancyIndicatorState(): string {
    if (!this.summary) return '';
    if (this.summary.vendorsWithShortageCount > 0) return 'falta';
    if (this.summary.vendorsWithSurplusCount > 0) return 'sobra';
    return 'cuadra';
  }

  stepNumClass(step: StepSummaryItemDto): string {
    if (step.hasDiscrepancy) return 'jr-p6-num--alerta';
    if (step.status === 'COMPLETED') return 'jr-p6-num--completo';
    if (step.status === 'IN_PROGRESS') return 'jr-p6-num--en-curso';
    return '';
  }

  stepChipClass(step: StepSummaryItemDto): string {
    if (step.hasDiscrepancy) return 'jr-chip--aviso';
    if (step.status === 'COMPLETED') return 'jr-chip--cuadra';
    if (step.status === 'IN_PROGRESS') return 'jr-chip--en-curso';
    return 'jr-chip--pendiente';
  }

  stepChipLabel(step: StepSummaryItemDto): string {
    if (step.hasDiscrepancy) return 'Con descuadres';
    if (step.status === 'COMPLETED') return 'Completo';
    if (step.status === 'IN_PROGRESS') return 'En curso';
    if (step.status === 'LOCKED') return 'Bloqueado';
    return 'Pendiente';
  }

  routeForStep(stepNumber: number): string {
    const routes: Record<number, string> = {
      1: '/jornada/personal',
      2: '/jornada/vendedores',
      3: '/jornada/electronicos',
      4: '/jornada/caja',
      5: '/jornada/inventario'
    };
    return routes[stepNumber] || '/jornada/personal';
  }

  incompleteSteps(): StepSummaryItemDto[] {
    return (this.summary?.steps ?? []).filter(s => s.status !== 'COMPLETED');
  }

  incompleteStepsText(): string {
    return this.incompleteSteps()
      .map(s => `paso ${s.stepNumber} (${s.stepName.toLowerCase()})`)
      .join(', ');
  }

  stepsWithDiscrepancy(): StepSummaryItemDto[] {
    return (this.summary?.steps ?? []).filter(s => s.hasDiscrepancy);
  }

  stepsWithDiscrepancyText(): string {
    return this.stepsWithDiscrepancy()
      .map(s => `paso ${s.stepNumber}`)
      .join(', ');
  }

  fuelVariations(): FuelVariationSummaryItemDto[] {
    return this.summary?.fuelVariations ?? [];
  }

  newAutonomyAlerts(): AutonomyAlertSummaryItemDto[] {
    return this.summary?.newAutonomyAlerts ?? [];
  }

  openCloseDialog(): void {
    if (this.isClosed()) return;
    this.isConfirmDialogOpen = true;
    this.cdr.detectChanges();
  }

  closeConfirmDialog(): void {
    if (this.isClosing) return;
    this.isConfirmDialogOpen = false;
    this.cdr.detectChanges();
  }

  confirmCloseJournal(): void {
    const journalId = this.journal?.id ?? this.summary?.journalId;
    if (!journalId || this.isClosing || this.isClosed()) return;

    this.isClosing = true;
    this.errorMessage = '';
    this.journalService.closeJournal(journalId).subscribe({
      next: () => {
        this.isClosing = false;
        this.isConfirmDialogOpen = false;
        this.router.navigate(['/jornada/cerrada']);
      },
      error: (err) => {
        this.isClosing = false;
        this.isConfirmDialogOpen = false;
        this.errorMessage = err?.error?.message || 'No se pudo cerrar la jornada.';
        this.cdr.detectChanges();
      }
    });
  }

  formatNumber(value: number | null | undefined, decimals = 2): string {
    if (value === null || value === undefined || isNaN(value)) return '—';
    return new Intl.NumberFormat('es-CO', {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals
    }).format(value);
  }
}
