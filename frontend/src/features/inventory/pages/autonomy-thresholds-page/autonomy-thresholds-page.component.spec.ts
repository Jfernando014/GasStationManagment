import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { AutonomyThresholdsService } from '../../../../core/services/autonomy-thresholds.service';
import { ApiError } from '../../../../shared/models/api-error.model';
import { AutonomyThresholdResponse } from '../../../../shared/models/dtos/autonomy-threshold.dto';
import { AutonomyThresholdsPageComponent } from './autonomy-thresholds-page.component';

const THRESHOLDS: AutonomyThresholdResponse[] = [
  {
    id: 2,
    validFrom: '2026-08-01',
    warningDays: 5,
    criticalDays: 2,
    createdAt: '2026-07-25T10:00:00'
  },
  {
    id: 1,
    validFrom: '2026-01-01',
    warningDays: 4,
    criticalDays: 1.5,
    createdAt: '2025-12-20T09:00:00'
  }
];

describe('AutonomyThresholdsPageComponent', () => {
  let thresholdsService: {
    list: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    thresholdsService = {
      list: vi.fn(() => of(THRESHOLDS)),
      create: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [AutonomyThresholdsPageComponent],
      providers: [
        provideRouter([]),
        { provide: AutonomyThresholdsService, useValue: thresholdsService }
      ]
    }).compileComponents();
  });

  function create() {
    const fixture = TestBed.createComponent(AutonomyThresholdsPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  function fill(
    component: AutonomyThresholdsPageComponent,
    validFrom: string,
    warningDays: number | null,
    criticalDays: number | null
  ): void {
    component.updateForm('validFrom', validFrom);
    component.updateForm('warningDays', warningDays);
    component.updateForm('criticalDays', criticalDays);
  }

  it('lists the registered validity dates with their thresholds', () => {
    const fixture = create();
    const text = fixture.nativeElement.textContent as string;

    expect(thresholdsService.list).toHaveBeenCalled();
    expect(text).toContain('01/08/2026');
    expect(text).toContain('5 días');
    expect(text).toContain('2 días');
  });

  it('rejects a critical threshold that is not lower than the warning one', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    fill(component, '2026-09-01', 3, 3);
    component.save();

    expect(component.isFieldInvalid('criticalDays')).toBe(true);
    expect(component.validationMessage('criticalDays')).toContain('menor que el de advertencia');
    expect(thresholdsService.create).not.toHaveBeenCalled();
  });

  it('saves a new validity date and reloads the list', () => {
    thresholdsService.create.mockReturnValue(of({
      id: 3,
      validFrom: '2026-09-01',
      warningDays: 6,
      criticalDays: 2.5,
      createdAt: '2026-08-30T08:00:00'
    }));
    const fixture = create();
    const component = fixture.componentInstance;

    fill(component, '2026-09-01', 6, 2.5);
    component.save();

    expect(thresholdsService.create).toHaveBeenCalledWith({
      validFrom: '2026-09-01',
      warningDays: 6,
      criticalDays: 2.5
    });
    expect(thresholdsService.list).toHaveBeenCalledTimes(2);
    expect(component.toastText()).toContain('Umbrales registrados');
    expect(component.form()).toEqual({ validFrom: '', warningDays: null, criticalDays: null });
  });

  it('shows that a validity date already exists', () => {
    thresholdsService.create.mockReturnValue(throwError(() => new ApiError(409, {
      detail: 'Thresholds already exist for that validity date'
    })));
    const fixture = create();
    const component = fixture.componentInstance;

    fill(component, '2026-08-01', 5, 2);
    component.save();

    expect(component.fieldError('validFrom')).toContain('Ya existen umbrales con esa fecha de vigencia');
    expect(component.saving()).toBe(false);
  });
});
