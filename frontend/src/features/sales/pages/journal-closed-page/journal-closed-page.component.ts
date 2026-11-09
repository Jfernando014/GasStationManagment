import { Component, OnInit, inject, ChangeDetectorRef, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { JournalService } from '../../../../core/services/journal.service';
import { SalesVendorCloseService } from '../../../../core/services/sales-vendor-close.service';
import {
  AutonomyAlertSummaryItemDto,
  FuelVariationSummaryItemDto,
  JournalClosingSummaryResponse,
  JournalResponse,
  JournalStatus
} from '../../../../shared/models/journal.model';
import { VendorCloseSummaryDto } from '../../../../shared/models/dtos/vendor-close.dto';

export type ClosedJournalSubView = 'inicio' | 'rendimiento' | 'inventario';

@Component({
  selector: 'app-journal-closed-page',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './journal-closed-page.component.html',
  styleUrls: ['./journal-closed-page.component.css']
})
export class JournalClosedPageComponent implements OnInit {
  private readonly journalService = inject(JournalService);
  private readonly salesService = inject(SalesVendorCloseService, { optional: true });
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  journal: JournalResponse | null = null;
  summary: JournalClosingSummaryResponse | null = null;
  vendorCloses: VendorCloseSummaryDto[] = [];
  viewMode: ClosedJournalSubView = 'inicio';

  ngOnInit(): void {
    this.journalService.currentJournal$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(journal => {
        if (!journal) return;
        this.journal = journal;
        this.loadData(journal);
      });
  }

  private loadData(journal: JournalResponse): void {
    this.journalService.getClosingSummary(journal.id).subscribe({
      next: (res) => {
        this.summary = res;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cdr.detectChanges();
      }
    });

    if (this.salesService) {
      this.salesService.getVendorCloses(journal.date).subscribe({
        next: (closes) => {
          this.vendorCloses = closes || [];
          this.cdr.detectChanges();
        },
        error: () => {
          this.vendorCloses = [];
          this.cdr.detectChanges();
        }
      });
    }
  }

  isClosed(): boolean {
    return this.journal?.status === JournalStatus.CLOSED || this.summary?.status === JournalStatus.CLOSED;
  }

  setViewMode(mode: ClosedJournalSubView): void {
    this.viewMode = mode;
    this.cdr.detectChanges();
  }

  openFullJournalReadOnly(): void {
    this.router.navigate(['/jornada/personal']);
  }

  formattedHeaderTitle(): string {
    const dateStr = this.journal?.date || this.summary?.journalDate;
    if (!dateStr || !dateStr.includes('-')) return 'Jornada cerrada';
    const [y, m, d] = dateStr.split('-').map(Number);
    const date = new Date(y, m - 1, d);
    const monthName = date.toLocaleDateString('es-CO', { month: 'long' });
    return `Jornada del ${d} de ${monthName} cerrada`;
  }

  closedTimeFormatted(): string {
    const raw = this.summary?.closedAt || this.journal?.closedAt;
    if (!raw) return '';
    const d = new Date(raw);
    if (isNaN(d.getTime())) return '';
    return d.toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' });
  }

  vendorGallonsSummaryText(): string {
    if (!this.vendorCloses.length) return 'Consulta el consolidado de cierres por vendedor';
    return 'Galones del día: ' + this.vendorCloses
      .map(v => {
        const firstName = (v.vendorName || '').split(' ')[0];
        const totalGal = Number(v.gallonsMotor || 0) + Number(v.gallonsDiesel || 0) + Number(v.gallonsExtra || 0) + Number(v.gallonsMaxpro || 0);
        return `${firstName} ${this.formatNumber(totalGal, 1)}`;
      })
      .join(' · ');
  }

  inventorySummaryText(): string {
    const alertsCount = this.summary?.newAlertsCount ?? 0;
    const alertsPart = alertsCount > 0
      ? `${alertsCount} alerta${alertsCount === 1 ? '' : 's'} de autonomía`
      : 'sin alertas de autonomía';
    return `Variación de tanques y existencias · ${alertsPart}`;
  }

  totalVendorGallons(close: VendorCloseSummaryDto): number {
    return Number(close.gallonsMotor || 0)
      + Number(close.gallonsDiesel || 0)
      + Number(close.gallonsExtra || 0)
      + Number(close.gallonsMaxpro || 0);
  }

  fuelVariations(): FuelVariationSummaryItemDto[] {
    return this.summary?.fuelVariations ?? [];
  }

  autonomyAlerts(): AutonomyAlertSummaryItemDto[] {
    return this.summary?.newAutonomyAlerts ?? [];
  }

  formatCurrency(amount: number | null | undefined): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      maximumFractionDigits: 0
    }).format(amount || 0);
  }

  formatNumber(value: number | null | undefined, decimals = 1): string {
    if (value === null || value === undefined || isNaN(value)) return '—';
    return new Intl.NumberFormat('es-CO', {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals
    }).format(value);
  }
}
