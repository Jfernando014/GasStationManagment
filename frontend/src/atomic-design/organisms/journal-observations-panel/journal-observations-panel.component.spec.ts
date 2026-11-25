import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { JournalObservationsPanelComponent } from './journal-observations-panel.component';
import { JournalService } from '../../../core/services/journal.service';
import { JournalResponse, JournalStatus } from '../../../shared/models/journal.model';

describe('JournalObservationsPanelComponent (SR-08)', () => {
  let component: JournalObservationsPanelComponent;
  let fixture: ComponentFixture<JournalObservationsPanelComponent>;

  const journalServiceMock = {
    updateObservations: vi.fn()
  };

  const openJournal: JournalResponse = {
    id: 10,
    date: '2026-11-24',
    consecutive: 300,
    status: JournalStatus.OPEN,
    observations: null,
    steps: []
  };

  beforeEach(async () => {
    journalServiceMock.updateObservations.mockReset();

    await TestBed.configureTestingModule({
      imports: [JournalObservationsPanelComponent],
      providers: [{ provide: JournalService, useValue: journalServiceMock }]
    }).compileComponents();

    fixture = TestBed.createComponent(JournalObservationsPanelComponent);
    component = fixture.componentInstance;
  });

  it('starts collapsed on an open journal without notes and saves trimmed observations', () => {
    const updated: JournalResponse = {
      ...openJournal,
      observations: 'Calibración en surtidor 2.'
    };
    journalServiceMock.updateObservations.mockReturnValue(of(updated));

    fixture.componentRef.setInput('journal', openJournal);
    fixture.detectChanges();

    expect(component.isExpanded()).toBe(false);
    expect(component.statusLabel()).toBe('Sin notas');

    const toggleBtn = fixture.nativeElement.querySelector('#jr-obs-toggle-btn') as HTMLButtonElement;
    toggleBtn.click();
    fixture.detectChanges();

    expect(component.isExpanded()).toBe(true);
    component.onDraftChange('  Calibración en surtidor 2.  ');
    fixture.detectChanges();

    expect(component.hasUnsavedChanges()).toBe(true);
    expect(component.statusLabel()).toBe('Sin guardar');

    const saveBtn = fixture.nativeElement.querySelector('#jr-obs-save-btn') as HTMLButtonElement;
    saveBtn.click();
    fixture.detectChanges();

    expect(journalServiceMock.updateObservations).toHaveBeenCalledWith(10, 'Calibración en surtidor 2.');
    expect(component.hasUnsavedChanges()).toBe(false);
    expect(component.statusLabel()).toBe('Guardada');
    expect(fixture.nativeElement.textContent).toContain('Observaciones de la jornada guardadas.');
  });

  it('allows discarding unsaved edits', () => {
    fixture.componentRef.setInput('journal', { ...openJournal, observations: 'Nota original' });
    fixture.detectChanges();

    component.toggleExpanded();
    fixture.detectChanges();
    component.onDraftChange('Borrador modificado');
    fixture.detectChanges();

    expect(component.hasUnsavedChanges()).toBe(true);
    const discardBtn = fixture.nativeElement.querySelector('#jr-obs-discard-btn') as HTMLButtonElement;
    discardBtn.click();
    fixture.detectChanges();

    expect(component.draft()).toBe('Nota original');
    expect(component.hasUnsavedChanges()).toBe(false);
  });

  it('shows observations in read-only mode when journal is closed', () => {
    const closedJournal: JournalResponse = {
      ...openJournal,
      status: JournalStatus.CLOSED,
      observations: 'Novedad verificada antes del cierre.'
    };
    fixture.componentRef.setInput('journal', closedJournal);
    fixture.detectChanges();

    expect(component.isClosed()).toBe(true);
    expect(component.isExpanded()).toBe(true);
    expect(fixture.nativeElement.querySelector('#jr-obs-textarea')).toBeNull();
    expect(fixture.nativeElement.querySelector('#jr-obs-save-btn')).toBeNull();
    expect(fixture.nativeElement.querySelector('#jr-obs-readonly-text')?.textContent).toContain(
      'Novedad verificada antes del cierre.'
    );
  });

  it('shows an error alert when saving observations fails', () => {
    journalServiceMock.updateObservations.mockReturnValue(throwError(() => new Error('Network error')));
    fixture.componentRef.setInput('journal', openJournal);
    fixture.detectChanges();

    component.toggleExpanded();
    fixture.detectChanges();
    component.onDraftChange('Nota con error');
    fixture.detectChanges();
    component.saveObservations();
    fixture.detectChanges();

    expect(component.isSaving()).toBe(false);
    expect(component.feedback()?.isError).toBe(true);
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeTruthy();
  });
});
