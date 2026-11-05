import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { DailyDownloadService } from './daily-download.service';
import { AuthService } from './auth.service';
import { authInterceptor } from '../interceptors/auth.interceptor';
import { errorInterceptor } from '../interceptors/error.interceptor';
import { ApiError } from '../../shared/models/api-error.model';
import { ReportAdaptationError } from '../../shared/helpers/daily-report-adapter.helpers';
import {
  buildBackendLubricantsSheet,
  buildBackendSalesSheet
} from '../../shared/helpers/daily-report-adapter.test-data';

/**
 * Uses the real interceptors, as the app does. The backend files are real Excel files with the
 * layout of the backend, and the service must return them adapted to the layout of the prototype.
 *
 * @author javiersolanop777
 */
describe('DailyDownloadService', () => {
  const varBase = 'http://localhost:8080/api/v1/sales/downloads';
  let atrService: DailyDownloadService;
  let atrHttp: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { getToken: () => 'test-token' } }
      ]
    });
    atrService = TestBed.inject(DailyDownloadService);
    atrHttp = TestBed.inject(HttpTestingController);
  });

  afterEach(() => atrHttp.verify());

  it('requests the PLANILLA VENTAS as a blob, with the token, and returns it adapted', async () => {
    const objResult = firstValueFrom(atrService.downloadSalesSheet('2026-10-24'));

    const objRequest = atrHttp.expectOne(`${varBase}/2026-10-24/sales-sheet`);
    expect(objRequest.request.method).toBe('GET');
    expect(objRequest.request.responseType).toBe('blob');
    expect(objRequest.request.headers.get('Authorization')).toBe('Bearer test-token');
    objRequest.flush(await buildBackendSalesSheet());

    const objFile = await objResult;
    expect(objFile.fileName).toBe('PLANILLA-VENTAS-2026-10-24.xlsx');
    expect(objFile.content.size).toBeGreaterThan(0);
  });

  it('requests LUBRICANTES Y VARIOS from its own endpoint and returns it adapted', async () => {
    const objResult = firstValueFrom(atrService.downloadLubricantsAndMisc('2026-10-24'));

    const objRequest = atrHttp.expectOne(`${varBase}/2026-10-24/lubricants-and-misc`);
    expect(objRequest.request.method).toBe('GET');
    objRequest.flush(await buildBackendLubricantsSheet());

    expect((await objResult).fileName).toBe('LUBRICANTES-Y-VARIOS-2026-10-24.xlsx');
  });

  it('fails with ReportAdaptationError when the backend file is not an Excel file', async () => {
    const objResult = firstValueFrom(atrService.downloadSalesSheet('2026-10-24'));
    atrHttp.expectOne(`${varBase}/2026-10-24/sales-sheet`).flush(new Blob(['not an excel']));
    await expect(objResult).rejects.toBeInstanceOf(ReportAdaptationError);
  });

  it('keeps the HTTP status when the error body is a blob', async () => {
    const objResult = firstValueFrom(atrService.downloadSalesSheet('2026-10-24'));
    atrHttp
      .expectOne(`${varBase}/2026-10-24/sales-sheet`)
      .flush(new Blob(['{"detail":"There is no closing data"}']), { status: 404, statusText: 'Not Found' });

    await expect(objResult).rejects.toBeInstanceOf(ApiError);
    await expect(objResult).rejects.toMatchObject({ status: 404 });
  });
});
