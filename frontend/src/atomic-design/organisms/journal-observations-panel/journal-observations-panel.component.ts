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

const MAX_OBSERVATIONS_LENGTH = 2000;

/**
 * Reusable collapsible panel for operational observations across every step of a journal (SR-08).
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
  readonly isExpanded = signal(false);
  readonly isSaving = signal(false);
  readonly draft = signal('');
  readonly feedback = signal<PanelFeedback | null>(null);

  private lastJournalId: number | null = null;
  private lastSavedText = '';

  @Input()
  set journal(value: JournalResponse | null) {
    this.journalState.set(value);
    const savedText = (value?.observations ?? '').trim();
    const journalSwitched = (value?.id ?? null) !== this.lastJournalId;

    if (journalSwitched) {
      this.lastJournalId = value?.id ?? null;
      this.lastSavedText = savedText;
      this.draft.set(value?.observations ?? '');
      this.feedback.set(null);
      this.isExpanded.set(value?.status === JournalStatus.CLOSED && savedText.length > 0);
      return;
    }

    if (savedText !== this.lastSavedText && !this.hasUnsavedChanges()) {
      this.lastSavedText = savedText;
      this.draft.set(value?.observations ?? '');
    }
  }

  readonly isClosed = computed(() => this.journalState()?.status === JournalStatus.CLOSED);
  readonly savedObservations = computed(() => (this.journalState()?.observations ?? '').trim());
  readonly hasSavedObservations = computed(() => this.savedObservations().length > 0);
  readonly normalizedDraft = computed(() => this.draft().trim());
  readonly hasUnsavedChanges = computed(() => this.normalizedDraft() !== this.savedObservations());

  readonly previewText = computed(() => {
    if (!this.hasSavedObservations()) {
      return this.isClosed()
        ? 'No se registraron observaciones en esta jornada.'
        : 'Registra novedades, aclaraciones de descuadres o notas operativas del día.';
    }
    return this.savedObservations().replace(/\s+/g, ' ');
  });

  readonly statusLabel = computed(() => {
    if (this.isClosed()) {
      return this.hasSavedObservations() ? 'Solo lectura' : 'Sin notas';
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

  onDraftChange(value: string): void {
    this.draft.set(value);
    if (this.feedback()) {
      this.feedback.set(null);
    }
  }

  discardChanges(): void {
    this.draft.set(this.journalState()?.observations ?? '');
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

    this.isSaving.set(true);
    this.feedback.set(null);

    this.journalService
      .updateObservations(current.id, payload)
      .pipe(
        finalize(() => this.isSaving.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: updated => {
          this.journalState.set(updated);
          this.lastSavedText = (updated.observations ?? '').trim();
          this.draft.set(updated.observations ?? '');
          this.feedback.set({
            text: payload
              ? 'Observaciones de la jornada guardadas.'
              : 'Observaciones eliminadas.',
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
}
