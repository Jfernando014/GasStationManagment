import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, Validators } from '@angular/forms';

import { FormFieldComponent } from './form-field.component';

@Component({
  standalone: true,
  imports: [FormFieldComponent],
  template: `
    <app-form-field
      fieldId="demo"
      label="Campo"
      [control]="control"
      [min]="min()"
      [max]="max()"
      [autocomplete]="autocomplete()"
      [ariaLabel]="ariaLabel()"
      [invalid]="invalid()" />
  `
})
class HostComponent {
  readonly control = new FormControl('', { nonNullable: true, validators: Validators.required });
  readonly min = signal<string | null>(null);
  readonly max = signal<string | null>(null);
  readonly autocomplete = signal<string | null>(null);
  readonly ariaLabel = signal<string | null>(null);
  readonly invalid = signal(false);
}

describe('FormFieldComponent', () => {
  let fixture: ComponentFixture<HostComponent>;
  let host: HostComponent;

  const input = () => fixture.nativeElement.querySelector('input') as HTMLInputElement;
  const errorText = () => (fixture.nativeElement.querySelector('.field-error') as HTMLElement | null)?.textContent?.trim();

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [HostComponent] }).compileComponents();
    fixture = TestBed.createComponent(HostComponent);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('does not emit min, max or autocomplete when they are not given', () => {
    expect(input().hasAttribute('min')).toBe(false);
    expect(input().hasAttribute('max')).toBe(false);
    expect(input().hasAttribute('autocomplete')).toBe(false);
    expect(input().hasAttribute('aria-label')).toBe(false);
  });

  it('emits the aria-label when it is given', () => {
    host.ariaLabel.set('Fecha de observación 2');
    fixture.detectChanges();

    expect(input().getAttribute('aria-label')).toBe('Fecha de observación 2');
  });

  it('emits min, max and autocomplete when they are given', () => {
    host.min.set('2026-01-01');
    host.max.set('2026-12-31');
    host.autocomplete.set('off');
    fixture.detectChanges();

    expect(input().getAttribute('min')).toBe('2026-01-01');
    expect(input().getAttribute('max')).toBe('2026-12-31');
    expect(input().getAttribute('autocomplete')).toBe('off');
  });

  it('is not marked as invalid by default', () => {
    expect(input().classList.contains('is-invalid')).toBe(false);
    expect(input().hasAttribute('aria-invalid')).toBe(false);
  });

  it('forces is-invalid and aria-invalid when invalid is set', () => {
    host.invalid.set(true);
    fixture.detectChanges();

    expect(input().classList.contains('is-invalid')).toBe(true);
    expect(input().getAttribute('aria-invalid')).toBe('true');
    expect(errorText()).toBeUndefined();
  });

  it('keeps showing the control error message', () => {
    host.control.markAsDirty();
    host.control.markAsTouched();
    fixture.detectChanges();

    expect(errorText()).toBe('Este campo es obligatorio');
    expect(input().classList.contains('is-invalid')).toBe(true);
    expect(input().getAttribute('aria-invalid')).toBe('true');
    expect(input().getAttribute('aria-describedby')).toBe('demo-help');
  });

  it('keeps showing the control error message when invalid is also set', () => {
    host.invalid.set(true);
    host.control.markAsDirty();
    host.control.markAsTouched();
    fixture.detectChanges();

    expect(errorText()).toBe('Este campo es obligatorio');
    expect(input().classList.contains('is-invalid')).toBe(true);
  });
});
