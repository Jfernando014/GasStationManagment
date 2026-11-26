import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { signal } from '@angular/core';

import { AuthService } from '../../../../core/services/auth.service';
import { ColorThemeService } from '../../../../core/services/color-theme.service';
import { ToastService } from '../../../../core/services/toast.service';
import { WorkersService } from '../../../../core/services/workers.service';
import { DEFAULT_COLOR_THEME } from '../../../../shared/helpers/color-contrast.helpers';
import { ColorThemeDto } from '../../../../shared/models/dtos/color-settings.dto';
import { Worker } from '../../../../shared/models/dtos/worker.dto';
import { ColorsTabComponent } from './colors-tab.component';

const WORKERS: Worker[] = [
  {
    id: '11111111-1111-1111-1111-111111111111',
    fullName: 'Ana Milena Villamil',
    document: '1061700111',
    roleId: 'role-1',
    roleName: 'TITULAR',
    dispenser: 1,
    active: true
  },
  {
    id: '22222222-2222-2222-2222-222222222222',
    fullName: 'Carlos Andrés Paredes',
    document: '1061700222',
    roleId: 'role-2',
    roleName: 'APOYO',
    dispenser: 3,
    active: true
  }
];

describe('ColorsTabComponent', () => {
  let themeSignal: ReturnType<typeof signal<ColorThemeDto>>;
  let colorThemeStub: {
    theme: ReturnType<typeof signal<ColorThemeDto>>;
    fetchTheme: ReturnType<typeof vi.fn>;
    saveBatch: ReturnType<typeof vi.fn>;
    resetAll: ReturnType<typeof vi.fn>;
    registerWorkers: ReturnType<typeof vi.fn>;
    resolveWorkerColor: ReturnType<typeof vi.fn>;
  };
  let workersStub: { list: ReturnType<typeof vi.fn> };
  let authStub: { isAdmin: ReturnType<typeof vi.fn> };
  let toastStub: { show: ReturnType<typeof vi.fn>; hide: ReturnType<typeof vi.fn> };

  function setup(isAdmin = true) {
    themeSignal = signal<ColorThemeDto>(DEFAULT_COLOR_THEME);
    colorThemeStub = {
      theme: themeSignal,
      fetchTheme: vi.fn(() => of(themeSignal())),
      saveBatch: vi.fn((items: { category: string; entityKey: string; colorHex: string | null }[]) => {
        const next: ColorThemeDto = {
          ...themeSignal(),
          fuels: themeSignal().fuels.map(entry => {
            const match = items.find(i => i.category === 'FUEL' && i.entityKey === entry.key);
            return match && match.colorHex ? { ...entry, colorHex: match.colorHex, customized: true } : entry;
          })
        };
        themeSignal.set(next);
        return of(next);
      }),
      resetAll: vi.fn(() => {
        themeSignal.set(DEFAULT_COLOR_THEME);
        return of(DEFAULT_COLOR_THEME);
      }),
      registerWorkers: vi.fn(),
      resolveWorkerColor: vi.fn(() => null)
    };
    workersStub = { list: vi.fn(() => of(WORKERS)) };
    authStub = { isAdmin: vi.fn(() => isAdmin) };
    toastStub = { show: vi.fn(), hide: vi.fn() };

    TestBed.configureTestingModule({
      imports: [ColorsTabComponent],
      providers: [
        { provide: ColorThemeService, useValue: colorThemeStub },
        { provide: WorkersService, useValue: workersStub },
        { provide: AuthService, useValue: authStub },
        { provide: ToastService, useValue: toastStub }
      ]
    });

    const fixture = TestBed.createComponent(ColorsTabComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('renders fuels, shift families, and sellers and saves valid edits in a single batch', () => {
    const fixture = setup(true);
    const el = fixture.nativeElement as HTMLElement;

    expect(el.textContent).toContain('Corriente');
    expect(el.textContent).toContain('Maxpro');
    expect(el.textContent).toContain('Noche');
    expect(el.textContent).toContain('Ana Milena Villamil');

    const motorInput = el.querySelector<HTMLInputElement>('input[aria-label="Código hexadecimal para Corriente"]')!;
    motorInput.value = '#2563EB';
    motorInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const saveBtn = el.querySelector<HTMLButtonElement>('button.save-btn')!;
    expect(saveBtn.disabled).toBe(false);
    expect(saveBtn.textContent).toContain('(1)');

    saveBtn.click();
    fixture.detectChanges();

    expect(colorThemeStub.saveBatch).toHaveBeenCalledWith([
      { category: 'FUEL', entityKey: 'MOTOR', colorHex: '#2563EB' }
    ]);
    expect(toastStub.show).toHaveBeenCalledWith('Colores guardados y aplicados en todo el sistema.');
  });

  it('blocks saving when a low-contrast pale color is entered', () => {
    const fixture = setup(true);
    const el = fixture.nativeElement as HTMLElement;

    const motorInput = el.querySelector<HTMLInputElement>('input[aria-label="Código hexadecimal para Corriente"]')!;
    motorInput.value = '#FFFFFF';
    motorInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(el.textContent).toContain('Color demasiado claro');
    expect(el.querySelector<HTMLButtonElement>('button.save-btn')!.disabled).toBe(true);
  });

  it('assigns distinct curated colors to active workers and respects read-only mode for non-admins', () => {
    const fixture = setup(true);
    const el = fixture.nativeElement as HTMLElement;

    const assignBtn = Array.from(el.querySelectorAll<HTMLButtonElement>('button'))
      .find(btn => btn.textContent?.includes('Asignar paleta a activos'))!;
    assignBtn.click();
    fixture.detectChanges();

    const anaInput = el.querySelector<HTMLInputElement>('input[aria-label="Código hexadecimal para Ana Milena Villamil"]')!;
    const carlosInput = el.querySelector<HTMLInputElement>('input[aria-label="Código hexadecimal para Carlos Andrés Paredes"]')!;
    expect(anaInput.value).toMatch(/^#[0-9A-F]{6}$/);
    expect(carlosInput.value).toMatch(/^#[0-9A-F]{6}$/);
    expect(anaInput.value).not.toBe(carlosInput.value);

    TestBed.resetTestingModule();
    const readonlyFixture = setup(false);
    const readonlyEl = readonlyFixture.nativeElement as HTMLElement;
    expect(readonlyEl.textContent).toContain('Solo lectura');
    expect(readonlyEl.querySelector('button.save-btn')).toBeNull();
  });
});
