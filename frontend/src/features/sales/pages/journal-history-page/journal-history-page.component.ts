import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { JournalService } from '../../../../core/services/journal.service';
import {
  JournalHistoryDayDetailResponse,
  JournalHistoryDaySummaryDto,
  JournalHistoryMonthResponse
} from '../../../../shared/models/journal.model';

@Component({
  selector: 'app-journal-history-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './journal-history-page.component.html',
  styleUrls: ['./journal-history-page.component.css']
})
export class JournalHistoryPageComponent implements OnInit {
  private readonly journalService = inject(JournalService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly weekdays = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

  yearMonth = '2026-08';
  monthData: JournalHistoryMonthResponse | null = null;
  leadingEmptyCells: number[] = [];
  selectedMigratedDate: string | null = null;
  selectedDayDetail: JournalHistoryDayDetailResponse | null = null;
  isLoadingMonth = false;
  isLoadingDetail = false;

  ngOnInit(): void {
    const activeDate = this.journalService.getSelectedDate?.();
    if (activeDate && activeDate.length >= 7) {
      this.yearMonth = activeDate.slice(0, 7);
    }
    this.loadMonth(this.yearMonth);
  }

  loadMonth(period: string): void {
    if (!period) return;
    this.yearMonth = period;
    this.selectedMigratedDate = null;
    this.selectedDayDetail = null;
    this.computeLeadingEmptyCells(period);
    this.isLoadingMonth = true;

    this.journalService.getJournalHistory(period).subscribe({
      next: (res) => {
        this.monthData = res;
        this.isLoadingMonth = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.monthData = this.buildEmptyMonthFallback(period);
        this.isLoadingMonth = false;
        this.cdr.detectChanges();
      }
    });
  }

  private computeLeadingEmptyCells(period: string): void {
    const [y, m] = period.split('-').map(Number);
    if (!y || !m) {
      this.leadingEmptyCells = [];
      return;
    }
    const firstWeekdayMondayZero = (new Date(y, m - 1, 1).getDay() + 6) % 7;
    this.leadingEmptyCells = Array.from({ length: firstWeekdayMondayZero }, (_, i) => i);
  }

  private buildEmptyMonthFallback(period: string): JournalHistoryMonthResponse {
    const [y, m] = period.split('-').map(Number);
    const totalDays = new Date(y, m, 0).getDate();
    const days: JournalHistoryDaySummaryDto[] = [];
    for (let d = 1; d <= totalDays; d++) {
      const dateStr = `${period}-${String(d).padStart(2, '0')}`;
      days.push({
        date: dateStr,
        dayOfMonth: d,
        source: 'EMPTY',
        state: 'PENDING',
        journalId: null,
        consecutive: null,
        markers: []
      });
    }
    return {
      yearMonth: period,
      closedSystemCount: 0,
      migratedCount: 0,
      withMarkersCount: 0,
      days
    };
  }

  changeMonth(delta: number): void {
    const [y, m] = this.yearMonth.split('-').map(Number);
    const nextDate = new Date(y, m - 1 + delta, 1);
    const nextPeriod = `${nextDate.getFullYear()}-${String(nextDate.getMonth() + 1).padStart(2, '0')}`;
    this.loadMonth(nextPeriod);
  }

  onPeriodInputChange(newPeriod: string): void {
    if (newPeriod && /^\d{4}-\d{2}$/.test(newPeriod)) {
      this.loadMonth(newPeriod);
    }
  }

  onDayClick(day: JournalHistoryDaySummaryDto): void {
    if (day.source === 'MIGRATED' || day.state === 'MIGRATED') {
      this.selectedMigratedDate = day.date;
      this.isLoadingDetail = true;
      this.journalService.getJournalHistoryDayDetail(day.date).subscribe({
        next: (detail) => {
          this.selectedDayDetail = detail;
          this.isLoadingDetail = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.isLoadingDetail = false;
          this.cdr.detectChanges();
        }
      });
      return;
    }

    this.journalService.setSelectedDate?.(day.date);
    this.router.navigate(['/jornada/personal'], { queryParams: { date: day.date } });
  }

  closeMigratedPanel(): void {
    this.selectedMigratedDate = null;
    this.selectedDayDetail = null;
    this.cdr.detectChanges();
  }

  dayCssKey(day: JournalHistoryDaySummaryDto): string {
    if (day.source === 'MIGRATED' || day.state === 'MIGRATED') return 'migrada';
    if (day.state === 'CLOSED') return 'cerrada';
    if (day.state === 'OPEN') return 'en-curso';
    return 'pendiente';
  }

  dayLabel(day: JournalHistoryDaySummaryDto): string {
    if (day.source === 'MIGRATED' || day.state === 'MIGRATED') return 'Migrada';
    if (day.state === 'CLOSED') return 'Cerrada';
    if (day.state === 'OPEN') return 'En curso';
    return 'Pendiente';
  }

  markerCssKey(marker: string): string {
    return marker === 'DISCREPANCY' ? 'descuadre' : 'alerta';
  }

  markerTitle(marker: string): string {
    return marker === 'DISCREPANCY' ? 'Tuvo descuadres' : 'Alerta de autonomía';
  }

  longSpanishDate(iso: string): string {
    if (!iso || !iso.includes('-')) return iso;
    const [y, m, d] = iso.split('-').map(Number);
    const date = new Date(y, m - 1, d);
    const formatted = date.toLocaleDateString('es-CO', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    }).replace(',', '');
    return formatted.charAt(0).toUpperCase() + formatted.slice(1);
  }

  shortDayMonth(iso: string): string {
    if (!iso || !iso.includes('-')) return '';
    const [, m, d] = iso.split('-');
    return `${d}/${m}`;
  }

  shortVendorName(name: string): string {
    return (name || '').split(' ').slice(0, 2).join(' ');
  }

  shortFuelName(fuel: string): string {
    const map: Record<string, string> = {
      'Gasolina Corriente': 'Corriente',
      'Diésel ACPM': 'Diésel',
      'Gasolina Extra': 'Extra',
      'Diésel Max Pro': 'Max Pro'
    };
    return map[fuel] || fuel;
  }

  absValue(n: number): number {
    return Math.abs(n || 0);
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

