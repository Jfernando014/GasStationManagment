import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';

import { JournalStepChangesService } from '../services/journal-step-changes.service';

/** Asks confirmation before leaving a journal step that has unsaved edits. */
export const unsavedStepChangesGuard: CanDeactivateFn<unknown> = () =>
  inject(JournalStepChangesService).canDeactivateStep();
