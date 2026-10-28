import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { errorInterceptor } from './error.interceptor';
import { ApiError } from '../../shared/models/api-error.model';
import { getErrorMessage, getFieldErrors } from '../../shared/helpers/problem-details.helpers';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting()
      ]
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('keeps the inventory envelope ({ error, message, fields })', () => {
    let failure: unknown;
    http.put('/api/v1/inventory/measurements/2026-08-05', {}).subscribe({ error: e => (failure = e) });

    httpTesting.expectOne('/api/v1/inventory/measurements/2026-08-05').flush(
      {
        timestamp: '2026-08-05T10:00:00',
        status: 400,
        error: 'Invalid data',
        message: 'One or more fields failed validation',
        path: '/api/v1/inventory/measurements/2026-08-05',
        fields: [{ field: 'readings', message: 'The measurement must include at least one tank reading' }]
      },
      { status: 400, statusText: 'Bad Request' }
    );

    expect(failure).toBeInstanceOf(ApiError);
    expect(getErrorMessage(failure)).toBe('One or more fields failed validation');
    expect((failure as ApiError).title).toBe('Invalid data');
    expect(getFieldErrors(failure)).toEqual([
      { field: 'readings', message: 'The measurement must include at least one tank reading' }
    ]);
  });

  it('also reads RFC Problem Details ({ detail, errors })', () => {
    let failure: unknown;
    http.get('/api/test').subscribe({ error: e => (failure = e) });

    httpTesting.expectOne('/api/test').flush(
      { detail: 'No autorizado', code: 'ACCESS_DENIED', errors: [{ field: 'code', message: 'Requerido' }] },
      { status: 403, statusText: 'Forbidden' }
    );

    expect(failure).toBeInstanceOf(ApiError);
    expect((failure as ApiError).detail).toBe('No autorizado');
    expect((failure as ApiError).code).toBe('ACCESS_DENIED');
    expect(getFieldErrors(failure)).toHaveLength(1);
  });

  it('falls back to a generic message when the body is not recognized', () => {
    let failure: unknown;
    http.get('/api/test').subscribe({ error: e => (failure = e) });

    httpTesting.expectOne('/api/test').flush('<html>Bad gateway</html>', {
      status: 502,
      statusText: 'Bad Gateway'
    });

    expect(failure).toBeInstanceOf(ApiError);
    expect(getErrorMessage(failure)).toBe('Ocurrió un error inesperado. Intenta de nuevo');
    expect((failure as ApiError).status).toBe(502);
  });

  it('maps a connection failure to status zero', () => {
    let failure: unknown;
    http.get('/api/test').subscribe({ error: e => (failure = e) });

    httpTesting.expectOne('/api/test').error(new ProgressEvent('error'));

    expect(failure).toBeInstanceOf(ApiError);
    expect((failure as ApiError).status).toBe(0);
    expect(getErrorMessage(failure)).toBe('No se pudo conectar con el servidor');
  });
});
