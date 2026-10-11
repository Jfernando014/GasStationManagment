import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of } from 'rxjs';
import { routes } from '../../../app/app.routes';
import { AuthService } from '../../../core/services/auth.service';
import { SalesTargetsService } from '../../../core/services/sales-targets.service';

describe('C-14 dashboard route', () => {
  it('uses the existing protected layout and marks the new navigation link active', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes),
      { provide: AuthService, useValue: { hasToken: () => true, logout: () => undefined } },
      { provide: SalesTargetsService, useValue: { list: () => of([]) } }
    ] });
    const harness = await RouterTestingHarness.create('/resultados/metas');
    await harness.fixture.whenStable();
    harness.detectChanges();
    const layout = harness.routeNativeElement;
    expect(layout?.querySelectorAll('.app-floating-sidebar').length).toBe(1);
    expect(layout?.querySelectorAll('.app-floating-header').length).toBe(1);
    expect(layout?.querySelector('a[href="/resultados/metas"]')?.getAttribute('aria-current')).toBe('page');
    expect(layout?.querySelector('.trail-active')?.textContent).toContain('Resultados / Metas y cumplimiento');
    expect(layout?.querySelector('app-annual-goals-page h1')?.textContent).toBe('Metas y cumplimiento');
  });

  it('redirects unauthenticated access to login through the existing guard', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes),
      { provide: AuthService, useValue: { hasToken: () => false } }
    ] });
    const harness = await RouterTestingHarness.create('/resultados/metas');
    expect(harness.routeNativeElement?.querySelector('#view-login')).not.toBeNull();
    expect(harness.routeNativeElement?.querySelector('app-annual-goals-page')).toBeNull();
  });
});
