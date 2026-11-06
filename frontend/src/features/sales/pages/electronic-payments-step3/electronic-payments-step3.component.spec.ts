import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { JournalService } from '../../../../core/services/journal.service';
import { ElectronicPaymentsStatus } from '../../../../shared/models/dtos/electronic-payments.dto';
import { JournalResponse, JournalStatus, StepStatus } from '../../../../shared/models/journal.model';
import { ElectronicPaymentsStep3Component } from './electronic-payments-step3.component';

const SALES = 'http://localhost:8080/api/v1/sales';
const STATUS_URL = `${SALES}/electronic-payments/2026-08-05`;
const VOUCHERS_URL = `${STATUS_URL}/vendors/w1/vouchers`;
const REDEBAN_URL = `${SALES}/reconciliation/qr/2026-08-05/redeban`;
const POINTS_URL = `${SALES}/reconciliation/points/2026-08-05`;
const SUMMARY_URL = `${SALES}/reconciliation/qr/2026-08-05/summary/w1`;
const COMPLETE_URL = 'http://localhost:8080/api/v1/journals/1/steps/3/complete';

const NO_PERIOD = { mastercard: 0, visa: 0, amex: 0, qr: 0, totalEds: 0, source: 'NONE' };

function journal(step3 = StepStatus.IN_PROGRESS, step2 = StepStatus.COMPLETED): JournalResponse {
  return {
    id: 1,
    date: '2026-08-05',
    consecutive: 1,
    status: JournalStatus.OPEN,
    steps: [1, 2, 3, 4, 5, 6].map(number => ({
      id: number,
      stepNumber: number,
      name: `Paso ${number}`,
      status: number === 2 ? step2 : number === 3 ? step3 : StepStatus.PENDING,
      completedAt: null
    }))
  };
}

/** Carlos cuadra en todo; Ana tiene 19.760 de diferencia en Credibanco. */
function status(overrides: Partial<ElectronicPaymentsStatus> = {}): ElectronicPaymentsStatus {
  const base = {
    workerName: 'x',
    shiftCode: null,
    amex: 0,
    mastercardDawn: 0,
    visaDawn: 0,
    amexDawn: 0,
    planillaRedemptions: 0
  };
  return {
    date: '2026-08-05',
    canComplete: false,
    blockers: [{ code: 'CREDIBANCO_DIFFERENCE', message: 'Ana no cuadra en Credibanco.' }],
    vendors: [
      { ...base, workerId: 'w1', workerName: 'Carlos', shiftCode: 'DIA', mastercard: 900000, visa: 454436, planillaCredibanco: 1354436, planillaQr: 162606 },
      { ...base, workerId: 'w2', workerName: 'Ana', shiftCode: 'NOCHE', mastercard: 1500000, visa: 539520, planillaCredibanco: 2019760, planillaQr: 377902 }
    ],
    credibanco: {
      date: '2026-08-05',
      vendors: [
        { workerId: 'w1', workerName: 'Carlos', shiftCode: 'DIA', vouchersTotal: 1354436, planillaCredibanco: 1354436, difference: 0, matches: true },
        { workerId: 'w2', workerName: 'Ana', shiftCode: 'NOCHE', vouchersTotal: 2039520, planillaCredibanco: 2019760, difference: 19760, matches: false }
      ],
      allMatch: false,
      franchises: { amex: 0, mastercard: 0, visa: 0, amexDawn: 0, mastercardDawn: 0, visaDawn: 0, qr: 0 },
      qrAdded: false,
      totalEds: 3393956,
      accounting: { amex: 0, mastercard: 2400000, visa: 993956, qr: null, total: null },
      monthToDate: NO_PERIOD,
      previousMonth: NO_PERIOD,
      commissionRate: 0.0035,
      commissionBase: 3393956,
      commission: 11878.85
    },
    qr: {
      date: '2026-08-05',
      redebanLoaded: true,
      redebanTotal: 540508,
      planillaTotal: 540508,
      qrTotal: 540508,
      difference: 0,
      matchesDay: true,
      unassignedTotal: 0,
      perVendorReliable: true,
      byStatus: {},
      unassigned: [],
      vendors: [
        { workerId: 'w1', workerName: 'Carlos', shiftCode: 'DIA', redeban: 162606, planilla: 162606, difference: 0, matches: true },
        { workerId: 'w2', workerName: 'Ana', shiftCode: 'NOCHE', redeban: 377902, planilla: 377902, difference: 0, matches: true }
      ]
    },
    points: { date: '2026-08-05', declared: 0, pointsTotal: null, difference: null, status: 'COMPLETED_ZERO', source: null, vendors: [] },
    ...overrides
  };
}

describe('ElectronicPaymentsStep3Component', () => {
  let http: HttpTestingController;
  let journalService: JournalService;
  let fixture: ComponentFixture<ElectronicPaymentsStep3Component>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ElectronicPaymentsStep3Component],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
    journalService = TestBed.inject(JournalService);
  });

  afterEach(() => http.verify());

  function open(current = status(), currentJournal = journal()): HTMLElement {
    journalService.setCurrentJournal(currentJournal);
    fixture = TestBed.createComponent(ElectronicPaymentsStep3Component);
    fixture.detectChanges();
    http.expectOne(STATUS_URL).flush(current);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  function button(root: HTMLElement, text: string): HTMLButtonElement {
    const found = Array.from(root.querySelectorAll('button')).find(item => item.textContent?.includes(text));
    if (!found) throw new Error(`No hay un botón con el texto "${text}"`);
    return found;
  }

  function change(input: HTMLInputElement, value: string): void {
    input.value = value;
    input.dispatchEvent(new Event('change'));
  }

  it('shows the three tabs and how many vendors match', () => {
    const root = open();
    const tabs = Array.from(root.querySelectorAll('[role="tab"]')).map(item => item.textContent?.trim());
    expect(tabs).toEqual(['Credibanco', 'QR', 'Puntos Colombia']);
    expect(root.textContent).toContain('1 de 2 vendedores cuadran');
    expect(root.textContent).toContain('Carlos (DIA)');
    expect(root.textContent).toContain('Ana (NOCHE)');
    expect(root.querySelectorAll('.jr-chip--cuadra').length).toBeGreaterThan(0);
    expect(root.querySelectorAll('.jr-chip--falta').length).toBeGreaterThan(0);
  });

  it('lists what is still missing and keeps the step from completing', () => {
    const root = open();
    expect(root.textContent).toContain('Ana no cuadra en Credibanco.');
    expect(button(root, 'Marcar paso 3 completado').disabled).toBe(true);
  });

  it('completes the step when everything matches', () => {
    const root = open(status({ canComplete: true, blockers: [] }));
    const complete = button(root, 'Marcar paso 3 completado');
    expect(complete.disabled).toBe(false);

    complete.click();
    const call = http.expectOne(COMPLETE_URL);
    expect(call.request.method).toBe('POST');
    call.flush({ id: 3, stepNumber: 3, name: 'Paso 3', status: StepStatus.COMPLETED, completedAt: '2026-08-05T20:00:00' });
    fixture.detectChanges();

    expect(root.textContent).toContain('Paso completado');
    expect(fixture.componentInstance.completed()).toBe(true);
  });

  it('does not allow completing before step 2 is completed', () => {
    const root = open(status({ canComplete: true, blockers: [] }), journal(StepStatus.IN_PROGRESS, StepStatus.IN_PROGRESS));
    expect(button(root, 'Marcar paso 3 completado').disabled).toBe(true);
    expect(root.textContent).toContain('Ir al paso 2');
  });

  it('is read-only once the step is completed', () => {
    const root = open(status(), journal(StepStatus.COMPLETED));
    const input = root.querySelector('tbody tr input.jr-input') as HTMLInputElement;
    expect(input.disabled).toBe(true);
  });

  describe('Credibanco vouchers', () => {
    it('sends the six amounts of the row when one is edited', () => {
      const root = open();
      const input = root.querySelector('tbody tr input.jr-input') as HTMLInputElement;

      change(input, '950.000');

      const call = http.expectOne(VOUCHERS_URL);
      expect(call.request.method).toBe('PUT');
      expect(call.request.body).toEqual({
        mastercard: 950000,
        visa: 454436,
        amex: 0,
        mastercardDawn: 0,
        visaDawn: 0,
        amexDawn: 0
      });
      call.flush(status());
    });

    it('does not call the server when the amount did not change', () => {
      const root = open();
      const input = root.querySelector('tbody tr input.jr-input') as HTMLInputElement;
      change(input, '900000');
      http.expectNone(VOUCHERS_URL);
    });

    it('rejects an invalid amount and marks the cell', () => {
      const root = open();
      const input = root.querySelector('tbody tr input.jr-input') as HTMLInputElement;

      change(input, 'abc');
      fixture.detectChanges();

      http.expectNone(VOUCHERS_URL);
      expect(input.parentElement?.classList.contains('jr-cell--invalido')).toBe(true);
      expect(root.textContent).toContain('Digita un importe válido');
      expect(button(root, 'Marcar paso 3 completado').disabled).toBe(true);
    });

    it('sends two quick edits in order, the second carrying both changes', () => {
      const root = open();
      const inputs = root.querySelectorAll<HTMLInputElement>('tbody tr:first-child input.jr-input');

      change(inputs[0], '950.000');
      change(inputs[1], '500.000');

      const first = http.expectOne(VOUCHERS_URL);
      expect(first.request.body.mastercard).toBe(950000);
      expect(first.request.body.visa).toBe(454436);
      expect(http.match(VOUCHERS_URL).length).toBe(0);

      first.flush(status());

      const second = http.expectOne(VOUCHERS_URL);
      expect(second.request.body.mastercard).toBe(950000);
      expect(second.request.body.visa).toBe(500000);
      second.flush(status());
    });

    it('shows the accounting report with the commission', () => {
      const root = open();
      expect(root.textContent).toContain('Reporte contable del día');
      expect(root.querySelector('.is-comision')?.textContent).toContain('$11.879');
    });
  });

  describe('QR', () => {
    it('reads the WhatsApp text of the first vendor when the tab opens', () => {
      const root = open();
      button(root, 'QR').click();
      fixture.detectChanges();

      http.expectOne(SUMMARY_URL).flush({ text: '*Cierre QR 05/08/2026*\nVendedor: Carlos' });
      fixture.detectChanges();

      expect(root.querySelector('.jr-p3-burbuja')?.textContent).toContain('Cierre QR 05/08/2026');
      expect(button(root, 'Copiar texto').disabled).toBe(false);
    });

    it('uploads the CSV and reloads the step', () => {
      const root = open(status({ qr: { ...status().qr, redebanLoaded: false } }));
      button(root, 'QR').click();
      fixture.detectChanges();

      const file = new File(['a,b\n1,2'], 'redeban.csv', { type: 'text/csv' });
      fixture.componentInstance.onFileSelected({ target: { files: [file], value: '' } } as unknown as Event);

      const upload = http.expectOne(REDEBAN_URL);
      expect(upload.request.method).toBe('POST');
      expect((upload.request.body as FormData).get('file')).toBeInstanceOf(File);
      upload.flush({
        reconciliation: status().qr,
        totalRows: 10,
        accepted: 8,
        ignoredNotQr: 1,
        ignoredNotApproved: 1,
        ignoredOtherDay: 0
      });

      http.expectOne(STATUS_URL).flush(status());
      http.expectOne(SUMMARY_URL).flush({ text: 'texto' });
      fixture.detectChanges();

      expect(root.textContent).toContain('redeban.csv');
      expect(root.textContent).toContain('8 de 10 filas sumadas');
    });

    it('does not upload a file that is not a CSV', () => {
      const root = open();
      button(root, 'QR').click();
      http.expectOne(SUMMARY_URL).flush({ text: '' });

      const file = new File(['x'], 'redeban.xlsx');
      fixture.componentInstance.onFileSelected({ target: { files: [file], value: '' } } as unknown as Event);
      fixture.detectChanges();

      http.expectNone(REDEBAN_URL);
      expect(root.textContent).toContain('debe ser un CSV');
    });

    it('lists the transactions that could not be assigned', () => {
      const current = status();
      const root = open(
        status({
          qr: {
            ...current.qr,
            perVendorReliable: false,
            unassignedTotal: 15000,
            unassigned: [{ row: 4, time: '13:00:00', amount: 15000, reference: null, status: 'AMBIGUOUS' }]
          }
        })
      );
      button(root, 'QR').click();
      http.expectOne(SUMMARY_URL).flush({ text: '' });
      fixture.detectChanges();

      expect(root.textContent).toContain('Transacciones sin asignar');
      expect(root.textContent).toContain('Varios vendedores a esa hora');
      expect(root.textContent).toContain('comparación por vendedor es parcial');
    });
  });

  describe('Puntos Colombia', () => {
    const pending = (): ElectronicPaymentsStatus =>
      status({
        points: {
          date: '2026-08-05',
          declared: 41380,
          pointsTotal: null,
          difference: null,
          status: 'PENDING_TOTAL',
          source: null,
          vendors: [{ workerId: 'w1', workerName: 'Carlos', redemptions: 41380 }]
        }
      });

    it('says there is nothing to type when no vendor redeemed points', () => {
      const root = open();
      button(root, 'Puntos Colombia').click();
      fixture.detectChanges();
      expect(root.textContent).toContain('Hoy no hubo redenciones');
      expect(root.querySelector('.jr-p3-cuadre')).toBeNull();
    });

    it('saves the day total and reloads the step', () => {
      const root = open(pending());
      button(root, 'Puntos Colombia').click();
      fixture.detectChanges();
      expect(root.textContent).toContain('Falta digitar el total de Puntos Colombia');

      const input = root.querySelector('.jr-p3-cuadre input') as HTMLInputElement;
      input.value = '41.380';
      input.dispatchEvent(new Event('input'));
      button(root, 'Guardar total').click();

      const call = http.expectOne(POINTS_URL);
      expect(call.request.method).toBe('PUT');
      expect(call.request.body).toEqual({ total: 41380 });
      call.flush({});
      http.expectOne(STATUS_URL).flush(status());
    });

    it('does not save a blank total', () => {
      const root = open(pending());
      button(root, 'Puntos Colombia').click();
      fixture.detectChanges();

      button(root, 'Guardar total').click();
      fixture.detectChanges();

      http.expectNone(POINTS_URL);
      expect(root.textContent).toContain('Digita el total de Puntos Colombia');
    });
  });
});
