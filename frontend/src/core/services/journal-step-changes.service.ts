import { Injectable, signal } from '@angular/core';
import { Observable, Subject, of } from 'rxjs';
import { finalize, take } from 'rxjs/operators';

/** Contract implemented by journal steps that hold editable form drafts before saving to the backend. */
export interface StepDirtyHandler {
  hasUnsavedChanges(): boolean;
  /** Saves pending changes; emits true when navigation may proceed or false when validation/save fails. */
  saveBeforeExit(): Observable<boolean>;
}

/** Coordinates unsaved-changes confirmation across journal steps and navigation transitions. */
@Injectable({
  providedIn: 'root'
})
export class JournalStepChangesService {
  private activeHandler: StepDirtyHandler | null = null;
  private pendingDecision$: Subject<boolean> | null = null;

  readonly modalOpen = signal(false);
  readonly saving = signal(false);

  register(handler: StepDirtyHandler): void {
    this.activeHandler = handler;
  }

  unregister(handler: StepDirtyHandler): void {
    if (this.activeHandler === handler) {
      this.activeHandler = null;
    }
    this.cancelLeave();
  }

  hasUnsavedChanges(): boolean {
    return this.activeHandler?.hasUnsavedChanges() ?? false;
  }

  canDeactivateStep(): Observable<boolean> {
    if (!this.activeHandler || !this.activeHandler.hasUnsavedChanges()) {
      return of(true);
    }
    return this.requestLeaveDecision();
  }

  requestLeaveDecision(): Observable<boolean> {
    if (this.pendingDecision$) {
      this.pendingDecision$.next(false);
      this.pendingDecision$.complete();
    }
    this.pendingDecision$ = new Subject<boolean>();
    this.saving.set(false);
    this.modalOpen.set(true);
    return this.pendingDecision$.asObservable().pipe(take(1));
  }

  confirmSaveAndLeave(): void {
    if (this.saving() || !this.pendingDecision$) return;
    if (!this.activeHandler) {
      this.resolveDecision(true);
      return;
    }

    this.saving.set(true);
    this.activeHandler
      .saveBeforeExit()
      .pipe(
        take(1),
        finalize(() => this.saving.set(false))
      )
      .subscribe({
        next: saved => this.resolveDecision(saved),
        error: () => this.resolveDecision(false)
      });
  }

  confirmLeaveWithoutSaving(): void {
    if (this.saving()) return;
    this.resolveDecision(true);
  }

  cancelLeave(): void {
    if (this.saving() || !this.modalOpen()) return;
    this.resolveDecision(false);
  }

  private resolveDecision(allowNavigation: boolean): void {
    this.modalOpen.set(false);
    const subject = this.pendingDecision$;
    this.pendingDecision$ = null;
    if (subject) {
      subject.next(allowNavigation);
      subject.complete();
    }
  }
}
