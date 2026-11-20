import { Component, OnInit, inject, ChangeDetectorRef, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, NavigationEnd, Router, RouterModule } from '@angular/router';
import { filter } from 'rxjs';
import { JOURNAL_STEP_ROUTES, JournalService } from '../../../core/services/journal.service';
import { todayCalendarDay } from '../../../shared/helpers/format.helpers';
import { JournalResponse } from '../../../shared/models/journal.model';
import { JournalStepperComponent } from '../../organisms/journal-stepper/journal-stepper.component';
import { UnsavedStepModalComponent } from '../../organisms/unsaved-step-modal/unsaved-step-modal.component';

const STEP_ROUTES = JOURNAL_STEP_ROUTES;

@Component({
  selector: 'app-journal-page',
  standalone: true,
  imports: [CommonModule, RouterModule, JournalStepperComponent, UnsavedStepModalComponent],
  templateUrl: './journal-page.component.html',
  styleUrls: ['./journal-page.component.css']
})
export class JournalPageComponent implements OnInit {
  private journalService = inject(JournalService);
  private route = inject(ActivatedRoute, { optional: true });
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private destroyRef = inject(DestroyRef);

  journal: JournalResponse | null = null;
  errorMessage = '';
  currentStepIndex = -1;

  ngOnInit(): void {
    this.syncCurrentStepIndex();
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => {
        this.syncCurrentStepIndex();
        this.cdr.detectChanges();
      });

    const queryDate = this.route?.snapshot?.queryParamMap?.get('date');
    const selectedDate = (this.journalService as { getSelectedDate?: () => string | null }).getSelectedDate?.();
    const targetDate = queryDate || selectedDate || todayCalendarDay();
    this.journalService.openJournal(targetDate).subscribe({
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
      this.rememberCurrentStep();
      this.cdr.detectChanges();
    });
  }

  get hasPreviousStep(): boolean {
    return this.currentStepIndex > 0;
  }

  get hasNextStep(): boolean {
    return this.currentStepIndex >= 0 && this.currentStepIndex < STEP_ROUTES.length - 1;
  }

  get showStepFooter(): boolean {
    return this.currentStepIndex >= 0;
  }

  get currentStepNumber(): number {
    return this.currentStepIndex + 1;
  }

  goToPreviousStep(): void {
    if (!this.hasPreviousStep) return;
    const prevSegment = STEP_ROUTES[this.currentStepIndex - 1];
    void this.router.navigate(['/jornada', prevSegment]);
  }

  goToNextStep(): void {
    if (!this.hasNextStep) return;
    const nextSegment = STEP_ROUTES[this.currentStepIndex + 1];
    void this.router.navigate(['/jornada', nextSegment]);
  }

  private syncCurrentStepIndex(): void {
    const cleanUrl = this.router.url.split('?')[0].split('#')[0];
    const segments = cleanUrl.split('/').filter(Boolean);
    const last = segments[segments.length - 1] ?? '';
    this.currentStepIndex = STEP_ROUTES.indexOf(last as (typeof STEP_ROUTES)[number]);
    this.rememberCurrentStep();
  }

  private rememberCurrentStep(): void {
    if (this.currentStepIndex < 0 || !this.journal?.date) return;
    const stepRoute = STEP_ROUTES[this.currentStepIndex];
    (this.journalService as { rememberStep?: (date: string, step: string) => void }).rememberStep?.(
      this.journal.date,
      stepRoute
    );
  }
}
