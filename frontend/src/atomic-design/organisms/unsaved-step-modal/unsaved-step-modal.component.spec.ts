import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { JournalStepChangesService } from '../../../core/services/journal-step-changes.service';
import { UnsavedStepModalComponent } from './unsaved-step-modal.component';

describe('UnsavedStepModalComponent', () => {
  let service: JournalStepChangesService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UnsavedStepModalComponent]
    }).compileComponents();
    service = TestBed.inject(JournalStepChangesService);
  });

  it('shows Guardar cambios y salir, Salir sin guardar, top-right X and closes on backdrop click', () => {
    service.register({ hasUnsavedChanges: () => true, saveBeforeExit: () => of(true) });
    const resultSpy = vi.fn();
    service.canDeactivateStep().subscribe(resultSpy);

    const fixture = TestBed.createComponent(UnsavedStepModalComponent);
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('#unsavedStepModalTitle')?.textContent).toContain('Cambios sin guardar');
    const actionButtons = Array.from(el.querySelectorAll<HTMLButtonElement>('.unsaved-modal-actions button'));
    expect(actionButtons.map(b => b.textContent?.trim())).toEqual([
      'Salir sin guardar',
      'Guardar cambios y salir'
    ]);

    const backdrop = el.querySelector('.unsaved-modal-backdrop') as HTMLElement;
    backdrop.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();

    expect(resultSpy).toHaveBeenCalledWith(false);
    expect(el.querySelector('.unsaved-modal-backdrop')).toBeNull();
  });
});
