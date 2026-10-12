import { TestBed, ComponentFixture } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AnnualGoalsPageComponent } from './annual-goals-page.component';
import { SalesTargetsService } from '../../../core/services/sales-targets.service';
import { SalesTargetResponse } from '../../../shared/models/dtos/sales-target.dto';
import { ApiError } from '../../../shared/models/api-error.model';

describe('C-14 annual goals page', () => {
  let fixture: ComponentFixture<AnnualGoalsPageComponent>;
  let page: AnnualGoalsPageComponent;
  const service = { list: vi.fn(), create: vi.fn() };
  function target(year: number): SalesTargetResponse {
    return { id: '1', year, month: null, category: 'FUEL_AGGREGATE', unit: 'GALLONS',
      value: '123.456789012345', maximumSale: '200', createdAt: '2026-10-11T12:00:00Z' };
  }
  function createPage(): void {
    fixture = TestBed.createComponent(AnnualGoalsPageComponent);
    page = fixture.componentInstance;
    fixture.detectChanges();
  }
  function fill(): void {
    page.form.setValue({ value: '123.456789012345', maximumSale: '200' });
    page.requestSave();
  }
  beforeEach(() => {
    service.list.mockReset().mockReturnValue(of([]));
    service.create.mockReset();
    TestBed.configureTestingModule({ imports: [AnnualGoalsPageComponent],
      providers: [{ provide: SalesTargetsService, useValue: service }] });
  });

  it('starts without default amounts or invented sales, and opens the Metas tab with a closed accordion', () => {
    createPage();
    expect(page.form.getRawValue()).toEqual({ value: '', maximumSale: '' });
    expect(page.form.invalid).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Sin datos de ventas');
    const metas = fixture.nativeElement.querySelector('#tab-metas') as HTMLButtonElement;
    metas.click();
    fixture.detectChanges();
    expect(metas.getAttribute('aria-selected')).toBe('true');
    expect((fixture.nativeElement.querySelector('details') as HTMLDetailsElement).open).toBe(false);
    expect(page.monthlyFuel()).toBeNull();
    expect(page.margin()).toBeNull();
    const icons = fixture.nativeElement.querySelectorAll('app-empty-state .empty-state-icon svg') as NodeListOf<SVGElement>;
    expect(icons.length).toBe(3);
    for (const icon of icons) expect(icon.querySelector('path, rect')).not.toBeNull();
  });
  it('validates required and exact nonnegative decimals without imposing prototype-only maximum rules', () => {
    createPage();
    page.requestSave();
    expect(page.confirming()).toBe(false);
    page.form.setValue({ value: '-1', maximumSale: '1' });
    expect(page.form.invalid).toBe(true);
    page.form.setValue({ value: '1', maximumSale: '0' });
    expect(page.form.valid).toBe(true);
    expect(page.margin()).toBe('-1');
  });
  it('shows stored annual values in consultation mode, including unknown legacy maximumSale', () => {
    const year = new Date().getFullYear();
    service.list.mockReturnValue(of([{ ...target(year), maximumSale: null }]));
    createPage();
    expect(page.form.disabled).toBe(true);
    expect(page.form.controls.value.value).toBe('123.456789012345');
    expect(page.canRegister()).toBe(false);
    expect(page.margin()).toBeNull();
    page.confirmSave();
    expect(service.create).not.toHaveBeenCalled();
  });
  it('requires confirmation, prevents duplicate POSTs and queries again after success', () => {
    createPage();
    const result$ = new Subject<SalesTargetResponse>();
    service.create.mockReturnValue(result$);
    fill();
    expect(service.create).not.toHaveBeenCalled();
    page.confirmSave();
    page.confirmSave();
    expect(service.create).toHaveBeenCalledTimes(1);
    expect(page.saving()).toBe(true);
    expect(page.yearControl.disabled).toBe(true);
    service.list.mockReturnValue(of([target(page.year())]));
    result$.next(target(page.year()));
    expect(service.list).toHaveBeenCalledTimes(2);
    expect(page.form.disabled).toBe(true);
    expect(page.success()).toBe('Metas del año guardadas correctamente');
    expect(page.saving()).toBe(false);
  });
  it('keeps the successful registration locked if the verification GET fails', () => {
    createPage();
    service.create.mockReturnValue(of(target(page.year())));
    service.list.mockReturnValue(throwError(() => new ApiError(0, { detail: 'Sin conexión' })));
    fill();
    page.confirmSave();
    expect(page.annualTarget()).not.toBeNull();
    expect(page.form.disabled).toBe(true);
    expect(page.loadError()).toBe('Sin conexión');
    expect(page.success()).not.toBe('');
  });
  it('handles conflict by reading the existing record, without claiming successful registration', () => {
    createPage();
    service.create.mockReturnValue(throwError(() => new ApiError(409, { detail: 'Duplicada' })));
    service.list.mockReturnValue(of([target(page.year())]));
    fill();
    page.confirmSave();
    expect(page.saveError()).toContain('Ya existe');
    expect(page.success()).toBe('');
    expect(page.form.disabled).toBe(true);
  });
  it('preserves backend field validation and allows correction after a 400', () => {
    createPage();
    service.create.mockReturnValue(throwError(() => new ApiError(400, { detail: 'Solicitud inválida',
      errors: [{ field: 'value', message: 'Revisa la meta' }] })));
    fill();
    page.confirmSave();
    expect(page.form.controls.value.errors?.['server']).toBe('Revisa la meta');
    expect(page.form.enabled).toBe(true);
    expect(page.success()).toBe('');
    page.form.controls.value.setValue('100');
    expect(page.form.valid).toBe(true);
  });
  it.each([401, 403])('handles authorization failure %s without simulated success', status => {
    createPage();
    service.create.mockReturnValue(throwError(() => new ApiError(status, { detail: 'Autenticación' })));
    fill();
    page.confirmSave();
    expect(page.form.disabled).toBe(true);
    expect(page.canRegister()).toBe(false);
    expect(page.success()).toBe('');
    expect(page.saveError()).not.toBe('');
  });
  it('blocks registration when the API is absent, and supports retrying the query', () => {
    service.list.mockReturnValue(throwError(() => new ApiError(404, { detail: 'Not found' })));
    createPage();
    expect(page.loadError()).toContain('API de metas no está disponible');
    expect(page.canRegister()).toBe(false);
    service.list.mockReturnValue(of([]));
    page.retry();
    expect(page.canRegister()).toBe(true);
  });
  it.each([401, 403, 404, 0])('keeps query failure %s visible without fake targets or success', status => {
    service.list.mockReturnValue(throwError(() => new ApiError(status, { detail: 'No se pudo conectar con el servidor' })));
    createPage();
    fixture.detectChanges();
    expect(page.loadError()).not.toBe('');
    expect(page.ready()).toBe(false);
    expect(page.targets()).toEqual([]);
    expect(page.form.getRawValue()).toEqual({ value: '', maximumSale: '' });
    expect(page.canRegister()).toBe(false);
    expect(page.success()).toBe('');
    if (status === 403) {
      expect(page.loadError()).toContain('HTTP 403');
      expect(page.loadError()).toContain('permisos insuficientes');
      expect(page.loadError()).toContain('No se ha podido confirmar la causa');
      expect(page.loadError()).not.toContain('No tienes autorización');
    }
    if (status === 401) expect(page.loadError()).toContain('Inicia sesión');
    if (status === 404) expect(page.loadError()).toContain('API de metas no está disponible');
    if (status === 0) expect(page.loadError()).toBe('No se pudo conectar con el servidor');
  });
  it('cancels stale year queries and keeps monthly totals separate from annual data', () => {
    const first$ = new Subject<SalesTargetResponse[]>();
    const second$ = new Subject<SalesTargetResponse[]>();
    service.list.mockReturnValueOnce(first$).mockReturnValueOnce(second$);
    createPage();
    const firstYear = page.year();
    page.yearControl.setValue(firstYear + 1);
    first$.next([target(firstYear)]);
    expect(page.targets()).toEqual([]);
    second$.next([{ ...target(firstYear + 1), month: page.month(), value: '30' }]);
    expect(page.annualTarget()).toBeNull();
    expect(page.monthlyFuel()?.value).toBe('30');
    expect(page.form.controls.value.value).toBe('');
  });
});
