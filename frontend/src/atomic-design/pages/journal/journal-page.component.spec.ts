import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { JournalPageComponent } from './journal-page.component';
import { JournalService } from '../../../core/services/journal.service';
import { JournalResponse, JournalStatus, StepStatus } from '../../../shared/models/journal.model';

describe('JournalPageComponent (C-02, S1-15)', () => {
  let component: JournalPageComponent;
  let fixture: ComponentFixture<JournalPageComponent>;
  const journalService = { openJournal: vi.fn(), completeStep: vi.fn() };

  const mockJournal: JournalResponse = {
    id: 10,
    date: '2026-08-05',
    consecutive: 289,
    status: JournalStatus.OPEN,
    steps: [
      { id: 1, stepNumber: 1, name: 'Personal', status: StepStatus.COMPLETED, completedAt: '2026-08-05T08:00:00Z' },
      { id: 2, stepNumber: 2, name: 'Vendedores', status: StepStatus.PENDING, completedAt: null },
      { id: 3, stepNumber: 3, name: 'Electrónicos', status: StepStatus.PENDING, completedAt: null },
      { id: 4, stepNumber: 4, name: 'Caja', status: StepStatus.PENDING, completedAt: null },
      { id: 5, stepNumber: 5, name: 'Inventario', status: StepStatus.PENDING, completedAt: null },
      { id: 6, stepNumber: 6, name: 'Cierre', status: StepStatus.PENDING, completedAt: null }
    ]
  };

  beforeEach(async () => {
    journalService.openJournal.mockReset();
    journalService.completeStep.mockReset();

    await TestBed.configureTestingModule({
      imports: [JournalPageComponent],
      providers: [
        provideRouter([]),
        { provide: JournalService, useValue: journalService }
      ]
    }).compileComponents();
  });

  it('should fetch today journal on init and display header and open status badge', () => {
    journalService.openJournal.mockReturnValue(of(mockJournal));

    fixture = TestBed.createComponent(JournalPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.journal).toEqual(mockJournal);
    expect(journalService.openJournal).toHaveBeenCalled();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Jornada Diaria');
    expect(text).toContain('Planilla #289');
    expect(text).toContain('2026-08-05');
    expect(text).toContain('Abierta');

    // Stepper component and router-outlet are present
    expect(fixture.nativeElement.querySelector('app-journal-stepper')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('router-outlet')).toBeTruthy();
  });

  it('should display closed status badge when journal is closed', () => {
    const closedJournal = { ...mockJournal, status: JournalStatus.CLOSED };
    journalService.openJournal.mockReturnValue(of(closedJournal));

    fixture = TestBed.createComponent(JournalPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Cerrada');
  });

  it('should display error alert when journal loading fails', () => {
    journalService.openJournal.mockReturnValue(throwError(() => new Error('Server error')));

    fixture = TestBed.createComponent(JournalPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.errorMessage).toBe('No se pudo cargar la jornada de hoy.');
    const errorAlert = fixture.nativeElement.querySelector('.error-alert');
    expect(errorAlert).toBeTruthy();
    expect(errorAlert.textContent).toContain('No se pudo cargar la jornada de hoy.');
  });
});
