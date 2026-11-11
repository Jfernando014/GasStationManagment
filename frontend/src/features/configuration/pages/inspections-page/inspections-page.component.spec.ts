import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { InspectionsService } from '../../../../core/services/inspections.service';
import { ApiError } from '../../../../shared/models/api-error.model';
import { InspectionResponse } from '../../../../shared/models/dtos/inspection.dto';
import { InspectionsPageComponent } from './inspections-page.component';

const INSPECTIONS: InspectionResponse[] = [
  {
    id: 1,
    code: 'CAL-ONAC-2026-08',
    category: 'ONAC',
    target: 'Dispensador 1 y 2',
    dateExecuted: '2020-03-15',
    dateDue: '2026-09-15',
    certifier: 'Metrología del Cauca SAS',
    toleranceMeasured: '+3,8 mL (Límite ±15 mL)',
    status: 'PROXIMO',
    certDoc: 'CERT-ONAC-0812.pdf',
    createdAt: '2020-03-15T08:00:00',
    updatedAt: '2020-03-15T08:00:00'
  },
  {
    id: 2,
    code: 'HERM-TK-2026-01',
    category: 'HERMETICIDAD',
    target: 'Tanques 1, 2 y 3',
    dateExecuted: '2020-02-18',
    dateDue: '2027-02-18',
    certifier: 'CertiTanques Colombia SAS',
    toleranceMeasured: '0,000 bar/h (0 fugas)',
    status: 'VIGENTE',
    certDoc: null,
    createdAt: '2020-02-18T08:00:00',
    updatedAt: '2020-02-18T08:00:00'
  },
  {
    id: 3,
    code: 'AMB-CRC-2025-01',
    category: 'AMBIENTAL',
    target: 'Trampa de grasas',
    dateExecuted: '2019-05-12',
    dateDue: '2026-05-12',
    certifier: 'CRC',
    toleranceMeasured: 'Conforme PMA',
    status: 'VENCIDO',
    certDoc: 'CRC-AMB-2025.pdf',
    createdAt: '2019-05-12T08:00:00',
    updatedAt: '2019-05-12T08:00:00'
  }
];

describe('InspectionsPageComponent', () => {
  let inspectionsService: { list: ReturnType<typeof vi.fn>; create: ReturnType<typeof vi.fn>; update: ReturnType<typeof vi.fn> };

  function create() {
    const fixture = TestBed.createComponent(InspectionsPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(async () => {
    inspectionsService = {
      list: vi.fn(() => of(INSPECTIONS)),
      create: vi.fn(),
      update: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [InspectionsPageComponent],
      providers: [{ provide: InspectionsService, useValue: inspectionsService }]
    }).compileComponents();
  });

  it('loads the log with the nearest due date first and counts each status', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    expect(component.inspections().map(inspection => inspection.id)).toEqual([3, 1, 2]);
    expect(component.counts()).toEqual({ total: 3, vigentes: 1, proximos: 1, vencidos: 1 });
    expect(fixture.nativeElement.textContent).toContain('CAL-ONAC-2026-08');
    expect(fixture.nativeElement.textContent).toContain('Próximo a vencer');
  });

  it('filters by category and status without another request', () => {
    const component = create().componentInstance;

    component.onCategoryFilter('ONAC');
    expect(component.visibleInspections().map(inspection => inspection.id)).toEqual([1]);

    component.onStatusFilter('VENCIDO');
    expect(component.visibleInspections()).toEqual([]);

    component.clearFilters();
    expect(component.visibleInspections()).toHaveLength(3);
    expect(inspectionsService.list).toHaveBeenCalledTimes(1);
  });

  it('does not send the form when a required field is missing', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    component.openCreate();
    component.save();
    fixture.detectChanges();

    expect(inspectionsService.create).not.toHaveBeenCalled();
    expect(component.formError()).toBe('Revisa los datos marcados para guardar la inspección.');
    expect(fixture.nativeElement.textContent).toContain('El código del acta es obligatorio');
  });

  it('registers an inspection and sorts it by due date', () => {
    const fixture = create();
    const component = fixture.componentInstance;
    const created: InspectionResponse = {
      id: 4,
      code: 'CAL-ONAC-2026-10',
      category: 'ONAC',
      target: 'Dispensador 3 y 4',
      dateExecuted: '2020-04-10',
      dateDue: '2026-08-01',
      certifier: 'Metrología del Cauca SAS',
      toleranceMeasured: '+2,1 mL',
      status: 'VENCIDO',
      certDoc: null,
      createdAt: '2020-04-10T08:00:00',
      updatedAt: '2020-04-10T08:00:00'
    };
    inspectionsService.create.mockReturnValue(of(created));

    component.openCreate();
    component.draft.set({
      code: 'CAL-ONAC-2026-10',
      category: 'ONAC',
      target: 'Dispensador 3 y 4',
      dateExecuted: '2020-04-10',
      dateDue: '2026-08-01',
      certifier: 'Metrología del Cauca SAS',
      toleranceMeasured: '+2,1 mL',
      certDoc: ''
    });
    component.save();

    expect(inspectionsService.create).toHaveBeenCalledWith({
      code: 'CAL-ONAC-2026-10',
      category: 'ONAC',
      target: 'Dispensador 3 y 4',
      dateExecuted: '2020-04-10',
      dateDue: '2026-08-01',
      certifier: 'Metrología del Cauca SAS',
      toleranceMeasured: '+2,1 mL',
      certDoc: null
    });
    expect(component.inspections().map(inspection => inspection.id)).toEqual([3, 4, 1, 2]);
    expect(component.formOpen()).toBe(false);
  });

  it('edits an inspection and replaces it in the log', () => {
    const component = create().componentInstance;
    const updated: InspectionResponse = { ...INSPECTIONS[0], target: 'Dispensador 3 y 4' };
    inspectionsService.update.mockReturnValue(of(updated));

    component.openEdit(INSPECTIONS[0]);
    expect(component.editingId()).toBe(1);
    expect(component.draft().code).toBe('CAL-ONAC-2026-08');

    component.updateDraft('target', 'Dispensador 3 y 4');
    component.save();

    expect(inspectionsService.update).toHaveBeenCalledWith(1, expect.objectContaining({
      code: 'CAL-ONAC-2026-08',
      target: 'Dispensador 3 y 4'
    }));
    expect(component.inspections().find(inspection => inspection.id === 1)?.target)
      .toBe('Dispensador 3 y 4');
  });

  it('shows the backend conflict next to the code', () => {
    const fixture = create();
    const component = fixture.componentInstance;
    inspectionsService.create.mockReturnValue(
      throwError(() => new ApiError(409, { message: 'Conflict', fields: [] }))
    );

    component.openCreate();
    component.draft.set({
      code: 'CAL-ONAC-2026-08',
      category: 'ONAC',
      target: 'Dispensador 1 y 2',
      dateExecuted: '2020-03-15',
      dateDue: '2026-09-15',
      certifier: 'Metrología del Cauca SAS',
      toleranceMeasured: '+3,8 mL',
      certDoc: ''
    });
    component.save();
    fixture.detectChanges();

    expect(component.fieldError('code')).toBe('El código del acta ya está registrado.');
    expect(fixture.nativeElement.textContent).toContain('El código del acta ya está registrado.');
  });

  it('formats an ISO date as dd/MM/yyyy without shifting it', () => {
    const component = create().componentInstance;

    expect(component.formatDate('2026-09-15')).toBe('15/09/2026');
  });
});
