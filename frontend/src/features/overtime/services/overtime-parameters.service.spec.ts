import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { errorInterceptor } from '../../../core/interceptors/error.interceptor';
import { ParameterVersionDto } from '../models/overtime.dto';
import { OvertimeParametersService } from './overtime-parameters.service';

describe('OvertimeParametersService', () => {
  let service: OvertimeParametersService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(OvertimeParametersService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads parameter versions with descending numbers and trimmed reasons', () => {
    const dtos: ParameterVersionDto[] = [
      {
        id: 2,
        validFrom: '2099-01-01',
        validTo: null,
        current: false,
        nightStartHour: 19,
        restHours: 2,
        restMinShiftHours: 8,
        fixedHedoHours: 6,
        fixedHedoRoles: ['TITULAR'],
        reason: '   ',
        createdBy: 'admin',
        createdAt: '2026-11-01T10:00:00Z'
      },
      {
        id: 1,
        validFrom: '2025-11-01',
        validTo: '2098-12-31',
        current: true,
        nightStartHour: 21,
        restHours: 1,
        restMinShiftHours: 8,
        fixedHedoHours: 6,
        fixedHedoRoles: ['TITULAR'],
        reason: 'Inicial',
        createdBy: null,
        createdAt: '2025-11-01T08:00:00Z'
      }
    ];

    let result: unknown;
    service.getHistory().subscribe(response => (result = response));

    const req = httpMock.expectOne(r => r.method === 'GET' && r.url.endsWith('/api/v1/workers/overtime-parameters/history'));
    req.flush(dtos);

    expect(result).toMatchObject({
      outcome: 'OK',
      data: [
        { id: 2, number: 2, scheduled: true, reason: null },
        { id: 1, number: 1, current: true, scheduled: false, reason: 'Inicial' }
      ]
    });
  });

  it('omits blank reason on create and maps field errors and 403', () => {
    let createResult: unknown;
    service.create({
      validFrom: '2026-12-01',
      nightStartHour: 19,
      restHours: 2,
      restMinShiftHours: 8,
      fixedHedoHours: 6,
      fixedHedoRoles: ['TITULAR'],
      reason: '   '
    }).subscribe(response => (createResult = response));

    const postReq = httpMock.expectOne(r => r.method === 'POST' && r.url.endsWith('/api/v1/workers/overtime-parameters'));
    expect(postReq.request.body).not.toHaveProperty('reason');
    postReq.flush({}, { status: 201, statusText: 'Created' });
    expect(createResult).toEqual({ outcome: 'OK' });

    let dupResult: unknown;
    service.create({
      validFrom: '2026-12-01',
      nightStartHour: 19,
      restHours: 2,
      restMinShiftHours: 8,
      fixedHedoHours: 6,
      fixedHedoRoles: ['TITULAR'],
      reason: 'Cambio ley'
    }).subscribe(response => (dupResult = response));

    const dupReq = httpMock.expectOne(r => r.method === 'POST' && r.url.endsWith('/api/v1/workers/overtime-parameters'));
    expect(dupReq.request.body).toMatchObject({ reason: 'Cambio ley' });
    dupReq.flush({ detail: 'Ya existe una versión desde esa fecha', code: 'PARAMETER_VERSION_EXISTS' }, {
      status: 409,
      statusText: 'Conflict'
    });
    expect(dupResult).toEqual({
      outcome: 'ERROR',
      message: 'Ya existe una versión desde esa fecha',
      field: 'validFrom'
    });

    let forbiddenResult: unknown;
    service.create({
      validFrom: '2026-12-01',
      nightStartHour: 19,
      restHours: 2,
      restMinShiftHours: 8,
      fixedHedoHours: 6,
      fixedHedoRoles: ['TITULAR'],
      reason: ''
    }).subscribe(response => (forbiddenResult = response));

    httpMock.expectOne(r => r.method === 'POST' && r.url.endsWith('/api/v1/workers/overtime-parameters')).flush(
      { detail: 'Forbidden' },
      { status: 403, statusText: 'Forbidden' }
    );
    expect(forbiddenResult).toEqual({
      outcome: 'ERROR',
      message: 'No tienes permiso para crear versiones',
      field: null
    });
  });
});
