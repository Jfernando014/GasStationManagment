import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { JournalStepChangesService } from '../../../core/services/journal-step-changes.service';

/** Blurred modal asking whether to save changes, discard them, or stay on the current step. */
@Component({
  selector: 'app-unsaved-step-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './unsaved-step-modal.component.html',
  styleUrl: './unsaved-step-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown.escape)': 'onEscapeKey()'
  }
})
export class UnsavedStepModalComponent {
  readonly changesService = inject(JournalStepChangesService);

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.changesService.cancelLeave();
    }
  }

  onEscapeKey(): void {
    if (this.changesService.modalOpen()) {
      this.changesService.cancelLeave();
    }
  }
}
