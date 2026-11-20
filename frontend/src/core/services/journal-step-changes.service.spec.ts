import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { JournalStepChangesService, StepDirtyHandler } from './journal-step-changes.service';

describe('JournalStepChangesService', () => {
  let service: JournalStepChangesService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(JournalStepChangesService);
  });

  it('allows immediate navigation when no handler is dirty', () => {
    const cleanHandler: StepDirtyHandler = {
      hasUnsavedChanges: () => false,
      saveBeforeExit: () => of(true)
    };
    service.register(cleanHandler);

    let allowed = false;
    service.canDeactivateStep().subscribe(res => (allowed = res));

    expect(allowed).toBe(true);
    expect(service.modalOpen()).toBe(false);
  });

  it('opens modal when dirty and lets user cancel via X / backdrop click', () => {
    const dirtyHandler: StepDirtyHandler = {
      hasUnsavedChanges: () => true,
      saveBeforeExit: vi.fn(() => of(true))
    };
    service.register(dirtyHandler);

    let result: boolean | undefined;
    service.canDeactivateStep().subscribe(res => (result = res));

    expect(service.modalOpen()).toBe(true);
    service.cancelLeave();

    expect(service.modalOpen()).toBe(false);
    expect(result).toBe(false);
    expect(dirtyHandler.saveBeforeExit).not.toHaveBeenCalled();
  });

  it('allows leaving without saving when user confirms discard', () => {
    const dirtyHandler: StepDirtyHandler = {
      hasUnsavedChanges: () => true,
      saveBeforeExit: vi.fn(() => of(true))
    };
    service.register(dirtyHandler);

    let result: boolean | undefined;
    service.canDeactivateStep().subscribe(res => (result = res));
    service.confirmLeaveWithoutSaving();

    expect(service.modalOpen()).toBe(false);
    expect(result).toBe(true);
    expect(dirtyHandler.saveBeforeExit).not.toHaveBeenCalled();
  });

  it('saves changes before leaving and blocks exit if save fails', () => {
    const failHandler: StepDirtyHandler = {
      hasUnsavedChanges: () => true,
      saveBeforeExit: vi.fn(() => of(false))
    };
    service.register(failHandler);

    let result: boolean | undefined;
    service.canDeactivateStep().subscribe(res => (result = res));
    service.confirmSaveAndLeave();

    expect(failHandler.saveBeforeExit).toHaveBeenCalled();
    expect(service.modalOpen()).toBe(false);
    expect(result).toBe(false);

    const okHandler: StepDirtyHandler = {
      hasUnsavedChanges: () => true,
      saveBeforeExit: vi.fn(() => of(true))
    };
    service.register(okHandler);
    service.canDeactivateStep().subscribe(res => (result = res));
    service.confirmSaveAndLeave();

    expect(okHandler.saveBeforeExit).toHaveBeenCalled();
    expect(result).toBe(true);

    const errHandler: StepDirtyHandler = {
      hasUnsavedChanges: () => true,
      saveBeforeExit: vi.fn(() => throwError(() => new Error('Network fail')))
    };
    service.register(errHandler);
    service.canDeactivateStep().subscribe(res => (result = res));
    service.confirmSaveAndLeave();
    expect(result).toBe(false);
  });
});
