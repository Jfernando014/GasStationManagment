import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { DailyDownloadsComponent } from './daily-downloads.component';
import { DailyDownloadService } from '../../../core/services/daily-download.service';
import { ApiError } from '../../../shared/models/api-error.model';
import { DownloadedFile } from '../../../shared/models/daily-download.model';

/**
 * @author javiersolanop777
 */
describe('DailyDownloadsComponent', () => {
  const objFile: DownloadedFile = { fileName: 'planilla-ventas-2026-10-24.xlsx', content: new Blob(['x']) };
  let atrFixture: ComponentFixture<DailyDownloadsComponent>;
  let atrService: { downloadSalesSheet: ReturnType<typeof vi.fn>; downloadLubricantsAndMisc: ReturnType<typeof vi.fn> };

  const rows = (): HTMLElement[] => Array.from(atrFixture.nativeElement.querySelectorAll('.jr-p6-descarga'));
  const button = (prmIndex: number): HTMLButtonElement => rows()[prmIndex].querySelector('button') as HTMLButtonElement;
  const text = (): string => atrFixture.nativeElement.textContent as string;

  beforeEach(async () => {
    atrService = { downloadSalesSheet: vi.fn(), downloadLubricantsAndMisc: vi.fn() };
    URL.createObjectURL = vi.fn(() => 'blob:test');
    URL.revokeObjectURL = vi.fn();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    await TestBed.configureTestingModule({
      imports: [DailyDownloadsComponent],
      providers: [{ provide: DailyDownloadService, useValue: atrService }]
    }).compileComponents();
    atrFixture = TestBed.createComponent(DailyDownloadsComponent);
    atrFixture.componentRef.setInput('atrDate', '2026-10-24');
    atrFixture.detectChanges();
  });

  afterEach(() => vi.restoreAllMocks());

  it('shows the five files of the prototype in order', () => {
    const arrNames = rows().map((prmRow) => prmRow.querySelector('.jr-p6-descarga-nombre')?.textContent?.trim());
    const arrTypes = rows().map((prmRow) => prmRow.querySelector('.jr-p6-tipo')?.textContent?.trim());
    expect(arrNames).toEqual([
      'Dominus con saldo en caja',
      'PLANILLA VENTAS',
      'LUBRICANTES Y VARIOS',
      'VARIACIÓN',
      'COMPLEMENTO DOMINUS'
    ]);
    expect(arrTypes).toEqual(['PDF', 'XLSX', 'XLSX', 'XLSX', 'XLSX']);
  });

  it('enables only the files that the backend offers', () => {
    const arrDisabled = rows().map((_prmRow, prmIndex) => button(prmIndex).disabled);
    expect(arrDisabled).toEqual([true, false, false, true, true]);
  });

  it('downloads the PLANILLA VENTAS and shows the result', () => {
    atrService.downloadSalesSheet.mockReturnValue(of(objFile));

    button(1).click();
    atrFixture.detectChanges();

    expect(atrService.downloadSalesSheet).toHaveBeenCalledWith('2026-10-24');
    expect(URL.createObjectURL).toHaveBeenCalledTimes(1);
    expect(text()).toContain('PLANILLA VENTAS descargado');
    expect(button(1).disabled).toBe(false);
  });

  it('downloads LUBRICANTES Y VARIOS with its own request', () => {
    atrService.downloadLubricantsAndMisc.mockReturnValue(of(objFile));

    button(2).click();
    atrFixture.detectChanges();

    expect(atrService.downloadLubricantsAndMisc).toHaveBeenCalledWith('2026-10-24');
    expect(atrService.downloadSalesSheet).not.toHaveBeenCalled();
    expect(text()).toContain('LUBRICANTES Y VARIOS descargado');
  });

  it('shows the generating state while the file is being built', () => {
    const objPending = new Subject<DownloadedFile>();
    atrService.downloadSalesSheet.mockReturnValue(objPending);

    button(1).click();
    atrFixture.detectChanges();
    expect(text()).toContain('Generando…');
    expect(button(1).disabled).toBe(true);
    expect(button(1).getAttribute('aria-busy')).toBe('true');
    expect(button(2).disabled).toBe(false);

    objPending.next(objFile);
    objPending.complete();
    atrFixture.detectChanges();
    expect(text()).not.toContain('Generando…');
    expect(button(1).disabled).toBe(false);
  });

  it('explains that the journal has no closing data (404)', () => {
    atrService.downloadSalesSheet.mockReturnValue(throwError(() => new ApiError(404, { detail: 'x' })));

    button(1).click();
    atrFixture.detectChanges();

    const objAlert = atrFixture.nativeElement.querySelector('[role="alert"]') as HTMLElement;
    expect(objAlert.textContent).toContain('aún no tiene cierres');
    expect(button(1).disabled).toBe(false);
  });

  it('does not call the backend for files that are not available yet', () => {
    button(0).click();
    button(3).click();
    button(4).click();
    expect(atrService.downloadSalesSheet).not.toHaveBeenCalled();
    expect(atrService.downloadLubricantsAndMisc).not.toHaveBeenCalled();
  });

  it('disables every button when there is no journal date', () => {
    atrFixture.componentRef.setInput('atrDate', '');
    atrFixture.detectChanges();
    expect(rows().every((_prmRow, prmIndex) => button(prmIndex).disabled)).toBe(true);
  });
});
