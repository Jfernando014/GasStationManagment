import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
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

  journal: JournalResponse | null = null;
  errorMessage = '';

  ngOnInit(): void {
    const today = new Date().toISOString().split('T')[0]; // Format YYYY-MM-DD
    this.journalService.openJournal(today).subscribe({
      next: (res) => {
        this.journal = res;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error opening journal:', err);
        this.errorMessage = 'No se pudo cargar la jornada de hoy.';
        this.cdr.detectChanges();
      }
    });
  }
}
