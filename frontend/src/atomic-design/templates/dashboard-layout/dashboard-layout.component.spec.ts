import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { vi } from 'vitest';

import { AuthService } from '../../../core/services/auth.service';
import { InventoryAlertsService } from '../../../core/services/inventory-alerts.service';
import { DashboardLayoutComponent } from './dashboard-layout.component';

describe('DashboardLayoutComponent', () => {
  let authService: {
    getUserProfile: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
    changePassword: ReturnType<typeof vi.fn>;
  };
  let alertsService: {
    pending$: Subject<number>;
    refreshPending: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authService = {
      getUserProfile: vi.fn(() => ({
        username: 'admin',
        displayName: 'Administrador',
        email: 'admin@edsbellavista.com',
        roleLabel: 'Administrador',
        initials: 'AD'
      })),
      logout: vi.fn(),
      changePassword: vi.fn()
    };
    alertsService = {
      pending$: new Subject<number>(),
      refreshPending: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [DashboardLayoutComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
        { provide: InventoryAlertsService, useValue: alertsService }
      ]
    }).compileComponents();
  });

  function create() {
    const fixture = TestBed.createComponent(DashboardLayoutComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('replaces header status and security buttons with the admin profile dropdown and single Metas link', () => {
    const fixture = create();
    const el = fixture.nativeElement as HTMLElement;

    expect(el.textContent).not.toContain('Operación Activa');
    expect(el.querySelector('.btn-change-password-header')).toBeNull();
    expect(el.querySelector('.app-floating-sidebar')?.textContent).not.toContain('Cambiar contraseña');

    const metasLinks = el.querySelectorAll('a[href*="/resultados/metas"], a[routerLink="/resultados/metas"]');
    expect(metasLinks.length).toBe(1);

    const trigger = el.querySelector('.admin-profile-trigger') as HTMLButtonElement;
    expect(trigger).toBeTruthy();
    expect(trigger.textContent).toContain('AD');
    expect(trigger.textContent).toContain('Administrador');
  });

  it('opens the dropdown with Mi perfil, Configuración, Seguridad and Cerrar sesión', () => {
    const fixture = create();
    const router = TestBed.inject(Router);
    const navSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const el = fixture.nativeElement as HTMLElement;

    const trigger = el.querySelector('.admin-profile-trigger') as HTMLButtonElement;
    trigger.click();
    fixture.detectChanges();

    const dropdown = el.querySelector('.admin-profile-dropdown') as HTMLElement;
    expect(dropdown).toBeTruthy();
    expect(dropdown.textContent).toContain('admin@edsbellavista.com');

    const items = Array.from(dropdown.querySelectorAll<HTMLButtonElement>('.admin-dropdown-item'));
    expect(items.map(b => b.textContent?.trim())).toEqual([
      'Mi perfil',
      'Configuración',
      'Seguridad',
      'Cerrar sesión'
    ]);

    items[0].click();
    fixture.detectChanges();
    expect(fixture.componentInstance.profileModalOpen()).toBe(true);
    expect(el.querySelector('#adminProfileTitle')?.textContent).toContain('Administrador');

    fixture.componentInstance.closeProfileModal();
    trigger.click();
    fixture.detectChanges();

    const reopenedItems = Array.from(el.querySelectorAll<HTMLButtonElement>('.admin-dropdown-item'));
    reopenedItems[1].click();
    expect(navSpy).toHaveBeenCalledWith(['/configuracion/parametros']);

    trigger.click();
    fixture.detectChanges();
    el.querySelectorAll<HTMLButtonElement>('.admin-dropdown-item')[2].click();
    expect(fixture.componentInstance.changePasswordModalOpen()).toBe(true);

    trigger.click();
    fixture.detectChanges();
    el.querySelectorAll<HTMLButtonElement>('.admin-dropdown-item')[3].click();
    expect(authService.logout).toHaveBeenCalled();
    expect(navSpy).toHaveBeenCalledWith(['/login']);
  });
});
