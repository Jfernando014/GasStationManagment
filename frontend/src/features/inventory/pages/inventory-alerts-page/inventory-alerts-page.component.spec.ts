import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';

import { InventoryAlertsService } from '../../../../core/services/inventory-alerts.service';
import { InventoryAlertResponse } from '../../../../shared/models/dtos/inventory-alert.dto';
import { InventoryAlertsPageComponent } from './inventory-alerts-page.component';

const ACTIVE: InventoryAlertResponse = {
  id: 9,
  alertType: 'AUTONOMY',
  fuelType: 'MOTOR',
  level: 'CRITICAL',
  status: 'ACTIVE',
  autonomyDays: 1.05,
  openedOn: '2026-11-06',
  createdAt: '2026-11-06T18:06:27',
  readAt: null,
  attendedAt: null,
  attendNote: null,
  resolvedAt: null
};

const READ: InventoryAlertResponse = {
  ...ACTIVE,
  id: 10,
  fuelType: 'DIESEL',
  level: 'WARNING',
  status: 'READ',
  autonomyDays: 4.2,
  readAt: '2026-11-06T19:00:00'
};

const RESOLVED: InventoryAlertResponse = {
  ...ACTIVE,
  id: 8,
  fuelType: 'EXTRA',
  status: 'RESOLVED',
  resolvedAt: '2026-11-05T08:00:00'
};

describe('InventoryAlertsPageComponent', () => {
  let alertsService: {
    list: ReturnType<typeof vi.fn>;
    setPending: ReturnType<typeof vi.fn>;
    markRead: ReturnType<typeof vi.fn>;
    attend: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    alertsService = {
      list: vi.fn((status?: string) => of(status === 'RESOLVED' ? [RESOLVED] : [ACTIVE, READ])),
      setPending: vi.fn(),
      markRead: vi.fn(() => of({ ...ACTIVE, status: 'READ', readAt: '2026-11-07T08:00:00' })),
      attend: vi.fn(() => of({ ...ACTIVE, status: 'ATTENDED', attendedAt: '2026-11-07T09:00:00' }))
    };

    await TestBed.configureTestingModule({
      imports: [InventoryAlertsPageComponent],
      providers: [
        provideRouter([]),
        { provide: InventoryAlertsService, useValue: alertsService }
      ]
    }).compileComponents();
  });

  function create() {
    const fixture = TestBed.createComponent(InventoryAlertsPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('lists the open alerts and keeps the bell counter in sync', () => {
    const fixture = create();
    const text = fixture.nativeElement.textContent as string;

    expect(alertsService.list).toHaveBeenCalledWith();
    expect(alertsService.list).toHaveBeenCalledWith('RESOLVED');
    expect(alertsService.setPending).toHaveBeenCalledWith(2);
    expect(text).toContain('Gasolina Corriente');
    expect(text).toContain('Diésel ACPM');
  });

  it('filters the tray by the selected status', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    component.setFilter('ACTIVE');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Gasolina Corriente');
    expect(fixture.nativeElement.textContent).not.toContain('Diésel ACPM');

    component.setFilter('RESOLVED');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Resuelta');
    expect(fixture.nativeElement.textContent).toContain('Gasolina Extra');
    expect(fixture.nativeElement.textContent).not.toContain('Gasolina Corriente');
  });

  it('marks an active alert as read and updates the pending counter', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    component.markRead(ACTIVE);
    fixture.detectChanges();

    expect(alertsService.markRead).toHaveBeenCalledWith(9);
    expect(component.alerts().find(alert => alert.id === 9)?.status).toBe('READ');
    // A read alert still counts as pending: only attending it removes it from the bell.
    expect(alertsService.setPending).toHaveBeenLastCalledWith(2);
  });

  it('rejects an attention note longer than 200 characters', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    component.attendTarget.set(ACTIVE);
    component.attendNote.set('x'.repeat(201));
    component.confirmAttend();

    expect(alertsService.attend).not.toHaveBeenCalled();
    expect(component.attendError()).toContain('200');
  });
});
