import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { JournalObservationsPanelComponent } from './journal-observations-panel.component';
import { JournalService } from '../../../core/services/journal.service';
import { JournalResponse, JournalStatus, StepStatus } from '../../../shared/models/journal.model';

describe('JournalObservationsPanelComponent (SR-08)', () => {
  let component: JournalObservationsPanelComponent;
  let fixture: ComponentFixture<JournalObservationsPanelComponent>;

  const journalServiceMock = {
    updateObservations: vi.fn(),
    updateStepObservations: vi.fn()
  };

  const openJournal: JournalResponse = {
    id: 10,
    date: '2026-11-24',
    consecutive: 300,
    status: JournalStatus.OPEN,
    observations: null,
    steps: [
      { id: 1, stepNumber: 1, name: 'Personal', status: StepStatus.COMPLETED, completedAt: '2026-11-24T06:00:00', observations: null },
      { id: 2, stepNumber: 2, name: 'Vendedores', status: StepStatus.PENDING, completedAt: null, observations: null },
      { id: 6, stepNumber: 6, name: 'Cierre', status: StepStatus.PENDING, completedAt: null, observations: null }
    ]
  };

  beforeEach(async () => {
    journalServiceMock.updateObservations.mockReset();
    journalServiceMock.updateStepObservations.mockReset();

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

  it('targets the active step, saves via updateStepObservations, and switches draft when step changes', () => {
    const journalWithStep1Note: JournalResponse = {
      ...openJournal,
      steps: [
        {
          id: 1,
          stepNumber: 1,
          name: 'Personal',
          status: StepStatus.COMPLETED,
          completedAt: '2026-11-24T06:00:00',
          observations: 'Turno de apoyo cubrió 10:00-17:00.'
        },
        {
          id: 2,
          stepNumber: 2,
          name: 'Vendedores',
          status: StepStatus.PENDING,
          completedAt: null,
          observations: null
        },
        {
          id: 6,
          stepNumber: 6,
          name: 'Cierre',
          status: StepStatus.PENDING,
          completedAt: null,
          observations: null
        }
      ]
    };

    const updatedAfterStep2: JournalResponse = {
      ...journalWithStep1Note,
      steps: journalWithStep1Note.steps.map(s =>
        s.stepNumber === 2 ? { ...s, observations: 'Parcial extra verificado con recibo.' } : s
      )
    };
    journalServiceMock.updateStepObservations.mockReturnValue(of(updatedAfterStep2));

    fixture.componentRef.setInput('journal', journalWithStep1Note);
    fixture.componentRef.setInput('activeStepNumber', 1);
    fixture.detectChanges();

    expect(component.panelTitle()).toBe('Observaciones · Paso 1: Personal del día');
    expect(component.draft()).toBe('Turno de apoyo cubrió 10:00-17:00.');
    expect(component.statusLabel()).toBe('Guardada');

    // Switch to Step 2
    fixture.componentRef.setInput('activeStepNumber', 2);
    fixture.detectChanges();

    expect(component.panelTitle()).toBe('Observaciones · Paso 2: Cierre por vendedor');
    expect(component.draft()).toBe('');
    expect(component.otherObservations()).toHaveLength(1);
    expect(fixture.nativeElement.querySelector('#jr-obs-other-pill')?.textContent).toContain('+1');

    component.toggleExpanded();
    component.onDraftChange('  Parcial extra verificado con recibo.  ');
    fixture.detectChanges();
    component.saveObservations();
    fixture.detectChanges();

    expect(journalServiceMock.updateStepObservations).toHaveBeenCalledWith(
      10,
      2,
      'Parcial extra verificado con recibo.'
    );
    expect(fixture.nativeElement.textContent).toContain('Observación del Paso 2 guardada.');

    // Switch to Step 6 -> shows consolidated observations of earlier steps automatically
    fixture.componentRef.setInput('activeStepNumber', 6);
    fixture.detectChanges();
    component.toggleExpanded();
    fixture.detectChanges();

    expect(component.shouldAutoShowOtherObservations()).toBe(true);
    expect(fixture.nativeElement.querySelector('#jr-obs-others')?.textContent).toContain(
      'Turno de apoyo cubrió 10:00-17:00.'
    );
    expect(fixture.nativeElement.querySelector('#jr-obs-others')?.textContent).toContain(
      'Parcial extra verificado con recibo.'
    );
  });
});
