import { DOCUMENT } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DEFAULT_COLOR_THEME } from '../../shared/helpers/color-contrast.helpers';
import { ColorThemeDto } from '../../shared/models/dtos/color-settings.dto';
import { ColorThemeService } from './color-theme.service';

describe('ColorThemeService', () => {
  let service: ColorThemeService;
  let httpMock: HttpTestingController;
  let doc: Document;

  beforeEach(() => {
    try {
      localStorage.removeItem('eds.appearance.color-theme.v1');
    } catch {
      // Ignore storage restrictions.
    }
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ColorThemeService);
    httpMock = TestBed.inject(HttpTestingController);
    doc = TestBed.inject(DOCUMENT);
  });

  afterEach(() => {
    httpMock.verify();
    service.clearSessionState();
  });

  it('injects fuel and shift CSS custom properties on fetchTheme and clears them on resetAll', () => {
    const customized: ColorThemeDto = {
      ...DEFAULT_COLOR_THEME,
      fuels: DEFAULT_COLOR_THEME.fuels.map(entry =>
        entry.key === 'MOTOR' ? { ...entry, colorHex: '#2563EB', customized: true } : entry
      ),
      shifts: DEFAULT_COLOR_THEME.shifts.map(entry =>
        entry.key === 'NOCHE' ? { ...entry, colorHex: '#7C3AED', customized: true } : entry
      ),
      workers: { '11111111-1111-1111-1111-111111111111': '#0F766E' }
    };

    service.fetchTheme().subscribe();
    httpMock.expectOne(req => req.url.endsWith('/api/v1/color-settings') && req.method === 'GET').flush(customized);

    expect(doc.documentElement.style.getPropertyValue('--fuel-corriente')).toBe('#2563EB');
    expect(doc.documentElement.style.getPropertyValue('--shift-noche')).toBe('#7C3AED');
    expect(doc.documentElement.style.getPropertyValue('--shift-noche-text')).toBe('#FFFFFF');

    service.resetAll().subscribe();
    httpMock.expectOne(req => req.url.endsWith('/api/v1/color-settings') && req.method === 'DELETE').flush(DEFAULT_COLOR_THEME);

    expect(doc.documentElement.style.getPropertyValue('--fuel-corriente')).toBe('');
    expect(doc.documentElement.style.getPropertyValue('--shift-noche')).toBe('');
  });

  it('resolves seller custom colors by workerId, document, or full name', () => {
    const workerId = '11111111-1111-1111-1111-111111111111';
    service.registerWorkers([
      { id: workerId, document: '1061700111', fullName: 'Ana Milena Villamil' }
    ]);

    const customized: ColorThemeDto = {
      ...DEFAULT_COLOR_THEME,
      workers: { [workerId]: '#F59E0B' }
    };
    service.fetchTheme().subscribe();
    httpMock.expectOne(req => req.url.endsWith('/api/v1/color-settings')).flush(customized);

    const byId = service.resolveWorkerColor({ workerId });
    const byDoc = service.resolveWorkerColor({ document: '1061700111' });
    const byName = service.resolveWorkerColor({ name: '  ana   milena villamil ' });

    expect(byId?.background).toBe('#F59E0B');
    expect(byId?.text).toBe('#0B192C');
    expect(byDoc?.background).toBe('#F59E0B');
    expect(byName?.background).toBe('#F59E0B');
    expect(service.resolveWorkerColor({ name: 'Sin Color' })).toBeNull();
  });
});
