import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { vi } from 'vitest';
import { JournalClosingStep6Component } from './journal-closing-step6.component';
import { JournalService } from '../../../../core/services/journal.service';
import { DailyDownloadService } from '../../../../core/services/daily-download.service';
import {
  JournalClosingSummaryResponse,
  JournalResponse,
  JournalStatus,
  StepStatus
} from '../../../../shared/models/journal.model';

describe('JournalClosingStep6Component (C-08, S3-05, S3-07)', () => {
  let component: JournalClosingStep6Component;
  let fixture: ComponentFixture<JournalClosingStep6Component>;
  let router: Router;

  const currentJournal$ = new BehaviorSubject<JournalResponse | null>(null);
  const journalServiceMock = {
    currentJournal$: currentJournal$.asObservable(),
    getClosingSummary: vi.fn(),
    closeJournal: vi.fn()
  };

  const dailyDownloadServiceMock = {
    downloadSalesSheet: vi.fn(),
    downloadLubricantsAndMisc: vi.fn()
  };

  const mockJournal: JournalResponse = {
    id: 10,
    date: '2026-08-05',
    consecutive: 289,
    status: JournalStatus.OPEN,
    steps: [
      { id: 1, stepNumber: 1, name: 'Personal', status: StepStatus.COMPLETED, completedAt: '2026-08-05T08:00:00Z' },
      { id: 2, stepNumber: 2, name: 'Vendedores', status: StepStatus.COMPLETED, completedAt: '2026-08-05T09:00:00Z' },
      { id: 3, stepNumber: 3, name: 'Electrónicos', status: StepStatus.COMPLETED, completedAt: '2026-08-05T10:00:00Z' },
      { id: 4, stepNumber: 4, name: 'Caja', status: StepStatus.PENDING, completedAt: null },
      { id: 5, stepNumber: 5, name: 'Inventario', status: StepStatus.COMPLETED, completedAt: '2026-08-05T11:00:00Z' },
      { id: 6, stepNumber: 6, name: 'Cierre', status: StepStatus.PENDING, completedAt: null }
    ]
  };

  const mockSummary: JournalClosingSummaryResponse = {
    journalId: 10,
    journalDate: '2026-08-05',
    consecutive: 289,
    status: JournalStatus.OPEN,
    closedAt: null,
    completedStepsCount: 4,
    vendorsWithShortageCount: 1,
    vendorsWithSurplusCount: 0,
    newAlertsCount: 1,
    steps: [
      { stepNumber: 1, stepName: 'Personal del día', status: 'COMPLETED', hasDiscrepancy: false, summaryDetail: '4 vendedores en turno' },
      { stepNumber: 2, stepName: 'Cierre por vendedor', status: 'COMPLETED', hasDiscrepancy: true, summaryDetail: 'Falta $5.000 Carlos' },
      { stepNumber: 3, stepName: 'Medios electrónicos', status: 'COMPLETED', hasDiscrepancy: false, summaryDetail: 'Credibanco $2.150.000' },
      { stepNumber: 4, stepName: 'Consolidado y caja', status: 'PENDING', hasDiscrepancy: false, summaryDetail: 'Pendiente por completar' },
      { stepNumber: 5, stepName: 'Tanques e inventario', status: 'COMPLETED', hasDiscrepancy: false, summaryDetail: 'Variación normal · 1 alerta de autonomía' }
    ],
    fuelVariations: [
      {
        fuelName: 'Gasolina Corriente',
        initialGallons: 4200,
        receivedGallons: 0,
        soldGallons: 580,
        theoreticalGallons: 3620,
        measuredGallons: 3616.5,
        variationGallons: 3.5,
        toleranceGallons: 18,
        status: 'NORMAL'
      }
    ],
    newAutonomyAlerts: [
      {
        tankId: 'T-03',
        fuelName: 'Gasolina Extra',
        autonomyDays: 2.5,
        reachesUntilDate: '2026-08-08',
        severity: 'WARNING'
      }
    ]
  };

  beforeEach(async () => {
    journalServiceMock.getClosingSummary.mockReset();
    journalServiceMock.closeJournal.mockReset();
    currentJournal$.next(mockJournal);

    await TestBed.configureTestingModule({
      imports: [JournalClosingStep6Component],
      providers: [
        provideRouter([]),
        { provide: JournalService, useValue: journalServiceMock },
        { provide: DailyDownloadService, useValue: dailyDownloadServiceMock }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
  });

  it('should render Step 6 summary, fuel variations, new autonomy alerts, and daily downloads', () => {
    journalServiceMock.getClosingSummary.mockReturnValue(of(mockSummary));

    fixture = TestBed.createComponent(JournalClosingStep6Component);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Cierre de jornada');
    expect(text).toContain('4 de 5');
    expect(text).toContain('1 falta');
    expect(text).toContain('Falta $5.000 Carlos');
    expect(text).toContain('Gasolina Corriente');
    expect(text).toContain('T-03 · Gasolina Extra');
    expect(fixture.nativeElement.querySelector('app-daily-downloads')).toBeTruthy();
  });

  it('should open confirmation dialog, confirm closing, and navigate to /jornada/cerrada', () => {
    journalServiceMock.getClosingSummary.mockReturnValue(of(mockSummary));
    journalServiceMock.closeJournal.mockReturnValue(of({
      ...mockJournal,
      status: JournalStatus.CLOSED,
      closedAt: '2026-08-05T18:45:00'
    }));
    const navSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    fixture = TestBed.createComponent(JournalClosingStep6Component);
    component = fixture.componentInstance;
    fixture.detectChanges();

    component.openCloseDialog();
    fixture.detectChanges();

    const dialogText = fixture.nativeElement.querySelector('#jr-p6-dialogo').textContent;
    expect(dialogText).toContain('Cerrar la jornada del 05/08');
    expect(dialogText).toContain('Sin completar: paso 4 (consolidado y caja)');
    expect(dialogText).toContain('Con descuadres: paso 2');

    component.confirmCloseJournal();
    expect(journalServiceMock.closeJournal).toHaveBeenCalledWith(10);
    expect(navSpy).toHaveBeenCalledWith(['/jornada/cerrada']);
  });
});
