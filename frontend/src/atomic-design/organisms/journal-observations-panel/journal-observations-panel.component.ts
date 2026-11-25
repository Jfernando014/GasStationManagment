import { CommonModule } from '@angular/common';
import { Component, DestroyRef, Input, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { ActionButtonDirective } from '../../atoms/action-button/action-button.directive';
import { TagComponent, TagVariant } from '../../atoms/tag/tag.component';
import { JournalService } from '../../../core/services/journal.service';
import { getErrorMessage } from '../../../shared/helpers/problem-details.helpers';
import { JournalResponse, JournalStatus } from '../../../shared/models/journal.model';

interface PanelFeedback {
  text: string;
  isError: boolean;
}

export interface ObservationSummaryItem {
  key: string;
  label: string;
  stepNumber: number | null;
  text: string;
}

const MAX_OBSERVATIONS_LENGTH = 2000;

const STEP_DISPLAY_NAMES: Record<number, string> = {
  1: 'Personal del día',
  2: 'Cierre por vendedor',
  3: 'Medios electrónicos',
  4: 'Saldo en caja',
  5: 'Tanques e inventario',
  6: 'Cierre de jornada'
};

/**
 * Reusable collapsible panel for per-step operational observations (Steps 1–6) and consolidated view (SR-08).
 * Editable while the journal is OPEN; read-only once CLOSED.
 */
@Component({
  selector: 'app-journal-observations-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, ActionButtonDirective, TagComponent],
  templateUrl: './journal-observations-panel.component.html',
  styleUrls: ['./journal-observations-panel.component.css']
})
export class JournalObservationsPanelComponent {
  private readonly journalService = inject(JournalService);
  private readonly destroyRef = inject(DestroyRef);

  readonly maxLength = MAX_OBSERVATIONS_LENGTH;

  readonly journalState = signal<JournalResponse | null>(null);
  readonly activeStepState = signal<number | null>(null);
  readonly isExpanded = signal(false);
  readonly showOtherNotes = signal(false);
  readonly isSaving = signal(false);
  readonly draft = signal('');
  readonly feedback = signal<PanelFeedback | null>(null);

  private lastContextKey = '';
  private lastSavedText = '';

  @Input()
  set journal(value: JournalResponse | null) {
    this.journalState.set(value);
    this.syncDraftWithContext();
  }

  @Input()
  set activeStepNumber(value: number | null | undefined) {
    const normalized = typeof value === 'number' && value >= 1 && value <= 6 ? value : null;
    this.activeStepState.set(normalized);
    this.syncDraftWithContext();
  }

  readonly isClosed = computed(() => this.journalState()?.status === JournalStatus.CLOSED);
  readonly isStepScoped = computed(() => {
    const step = this.activeStepState();
    return step !== null && step >= 1 && step <= 6;
  });

  readonly activeStepLabel = computed(() => {
    const step = this.activeStepState();
    if (!step) return '';
    return STEP_DISPLAY_NAMES[step] ?? `Paso ${step}`;
  });

  readonly panelTitle = computed(() => {
    const step = this.activeStepState();
    if (step !== null) {
      return `Observaciones · Paso ${step}: ${this.activeStepLabel()}`;
    }
    return 'Observaciones de la jornada';
  });

  readonly rawSavedObservations = computed(() => {
    const journal = this.journalState();
    if (!journal) return '';
    const stepNumber = this.activeStepState();
    if (stepNumber !== null) {
      const step = journal.steps?.find(item => item.stepNumber === stepNumber);
      return step?.observations ?? '';
    }
    return journal.observations ?? '';
  });

  readonly savedObservations = computed(() => this.rawSavedObservations().trim());
  readonly hasSavedObservations = computed(() => this.savedObservations().length > 0);
  readonly normalizedDraft = computed(() => this.draft().trim());
  readonly hasUnsavedChanges = computed(() => this.normalizedDraft() !== this.savedObservations());

  readonly allObservationsSummary = computed<ObservationSummaryItem[]>(() => {
    const journal = this.journalState();
    if (!journal) return [];
    const items: ObservationSummaryItem[] = [];
    const orderedSteps = [...(journal.steps ?? [])].sort((a, b) => a.stepNumber - b.stepNumber);
    for (const step of orderedSteps) {
      const text = (step.observations ?? '').trim();
      if (text.length > 0) {
        items.push({
          key: `step-${step.stepNumber}`,
          label: `Paso ${step.stepNumber} · ${STEP_DISPLAY_NAMES[step.stepNumber] ?? step.name}`,
          stepNumber: step.stepNumber,
          text
        });
      }
    }
    const general = (journal.observations ?? '').trim();
    if (general.length > 0) {
      items.push({
        key: 'general',
        label: 'Observación general de la jornada',
        stepNumber: null,
        text: general
      });
    }
    return items;
  });

  readonly otherObservations = computed<ObservationSummaryItem[]>(() => {
    const active = this.activeStepState();
    return this.allObservationsSummary().filter(item => item.stepNumber !== active);
  });

  readonly hasAnyObservations = computed(() => this.allObservationsSummary().length > 0);

  readonly shouldAutoShowOtherObservations = computed(
    () => (this.isClosed() || this.activeStepState() === 6 || !this.isStepScoped()) && this.otherObservations().length > 0
  );

  readonly previewText = computed(() => {
    if (this.hasSavedObservations()) {
      return this.savedObservations().replace(/\s+/g, ' ');
    }
    const othersCount = this.otherObservations().length;
    if (this.isClosed()) {
      if (othersCount > 0) {
        return `${othersCount} ${othersCount === 1 ? 'observación registrada' : 'observaciones registradas'} en esta jornada.`;
      }
      return 'No se registraron observaciones en esta jornada.';
    }
    if (this.isStepScoped()) {
      return `Agrega novedades o aclaraciones propias del Paso ${this.activeStepState()}.`;
    }
    return 'Registra novedades, aclaraciones de descuadres o notas operativas del día.';
  });

  readonly statusLabel = computed(() => {
    if (this.isClosed()) {
      return this.hasAnyObservations() ? 'Solo lectura' : 'Sin notas';
    }
    if (this.hasUnsavedChanges()) {
      return 'Sin guardar';
    }
    return this.hasSavedObservations() ? 'Guardada' : 'Sin notas';
  });

  readonly statusVariant = computed<TagVariant>(() => {
    if (this.isClosed()) {
      return 'gray';
    }
    if (this.hasUnsavedChanges()) {
      return 'amber';
    }
    return this.hasSavedObservations() ? 'green' : 'gray';
  });

  readonly toggleButtonLabel = computed(() => {
    if (this.isExpanded()) {
      return 'Ocultar';
    }
    if (this.isClosed()) {
      return 'Ver';
    }
    return this.hasSavedObservations() ? 'Editar' : 'Agregar nota';
  });

  toggleExpanded(): void {
    const next = !this.isExpanded();
    this.isExpanded.set(next);
    const currentFeedback = this.feedback();
    if (!next && currentFeedback && !currentFeedback.isError) {
      this.feedback.set(null);
    }
  }

  toggleOtherNotes(): void {
    this.showOtherNotes.update(current => !current);
  }

  onDraftChange(value: string): void {
    this.draft.set(value);
    if (this.feedback()) {
      this.feedback.set(null);
    }
  }

  discardChanges(): void {
    this.draft.set(this.rawSavedObservations());
    this.feedback.set(null);
  }

  onKeyDown(event: KeyboardEvent): void {
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') {
      event.preventDefault();
      this.saveObservations();
    }
  }

  saveObservations(): void {
    const current = this.journalState();
    if (!current || this.isClosed() || this.isSaving() || !this.hasUnsavedChanges()) {
      return;
    }

    const payload = this.normalizedDraft().length > 0 ? this.normalizedDraft() : null;
    if (payload && payload.length > this.maxLength) {
      this.feedback.set({
        text: `Las observaciones no pueden superar los ${this.maxLength} caracteres.`,
        isError: true
      });
      return;
    }

    const stepNumber = this.activeStepState();
    this.isSaving.set(true);
    this.feedback.set(null);

    const request$ = stepNumber !== null
      ? this.journalService.updateStepObservations(current.id, stepNumber, payload)
      : this.journalService.updateObservations(current.id, payload);

    request$
      .pipe(
        finalize(() => this.isSaving.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: updated => {
          this.journalState.set(updated);
          const freshRaw = this.rawSavedObservations();
          this.lastSavedText = freshRaw.trim();
          this.draft.set(freshRaw);
          this.feedback.set({
            text: stepNumber !== null
              ? (payload
                ? `Observación del Paso ${stepNumber} guardada.`
                : `Observación del Paso ${stepNumber} eliminada.`)
              : (payload
                ? 'Observaciones de la jornada guardadas.'
                : 'Observaciones eliminadas.'),
            isError: false
          });
        },
        error: (err: unknown) => {
          this.feedback.set({
            text: getErrorMessage(err) || 'No se pudieron guardar las observaciones.',
            isError: true
          });
        }
      });
  }

  private syncDraftWithContext(): void {
    const journal = this.journalState();
    const stepNumber = this.activeStepState();
    const contextKey = `${journal?.id ?? ''}:${stepNumber ?? 'general'}`;
    const rawSaved = this.rawSavedObservations();
    const trimmedSaved = rawSaved.trim();

    if (contextKey !== this.lastContextKey) {
      this.lastContextKey = contextKey;
      this.lastSavedText = trimmedSaved;
      this.draft.set(rawSaved);
      this.feedback.set(null);
      this.showOtherNotes.set(false);
      this.isExpanded.set(journal?.status === JournalStatus.CLOSED && this.hasAnyObservations());
      return;
    }

    if (trimmedSaved !== this.lastSavedText && !this.hasUnsavedChanges()) {
      this.lastSavedText = trimmedSaved;
      this.draft.set(rawSaved);
    }
  }
}
