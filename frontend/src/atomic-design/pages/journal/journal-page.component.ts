import { Component, OnInit, inject, ChangeDetectorRef, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { JournalService } from '../../../core/services/journal.service';
import { JournalResponse } from '../../../shared/models/journal.model';
import { JournalStepperComponent } from '../../organisms/journal-stepper/journal-stepper.component';

@Component({
  selector: 'app-journal-page',
  standalone: true,
  imports: [CommonModule, RouterModule, JournalStepperComponent],
  templateUrl: './journal-page.component.html',
  styleUrls: ['./journal-page.component.css']
})
export class JournalPageComponent implements OnInit {
  private journalService = inject(JournalService);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);

  journal: JournalResponse | null = null;
  errorMessage = '';

  ngOnInit(): void {
    const today = new Date().toISOString().split('T')[0]; // Format YYYY-MM-DD
    this.journalService.openJournal(today).subscribe({
      next: (res) => this.journalService.setCurrentJournal(res),
      error: (err) => {
        console.error('Error opening journal:', err);
        this.errorMessage = 'No se pudo cargar la jornada de hoy.';
        this.cdr.detectChanges();
      }
    });
    // Los pasos hijos (p. ej. personal del día) completan un paso a través de JournalService; este estado
    // compartido es la única fuente de la jornada actual para que lean su fecha sin recalcularla.
    this.journalService.currentJournal$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(journal => {
      if (!journal) return;
      this.journal = journal;
      this.cdr.detectChanges();
    });
  }
}
