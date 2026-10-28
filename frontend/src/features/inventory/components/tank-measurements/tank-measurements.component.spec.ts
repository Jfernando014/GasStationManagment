import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { formatGallons } from '../../../../shared/helpers/inventory-display.helpers';
import { TankResponse } from '../../../../shared/models/dtos/tank.dto';
import { TankMeasurementsComponent } from './tank-measurements.component';

const TANKS: TankResponse[] = [1, 2, 3].map(id => ({
  id,
  code: `T${id}`,
  name: `Tanque ${id}`,
  fuelType: 'MOTOR',
  maxCapacityGallons: 5000,
  maxHeightCm: 200,
  toleranceCm: 1,
  gaugeTableId: 1
}));

describe('TankMeasurementsComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TankMeasurementsComponent] }).compileComponents();
  });

  function create(heights: Record<number, number | null> = { 1: 100, 2: 250, 3: null }) {
    const fixture = TestBed.createComponent(TankMeasurementsComponent);
    fixture.componentRef.setInput('tanks', TANKS);
    fixture.componentRef.setInput('heights', heights);
    fixture.componentRef.setInput('gallons', { 1: 2000, 2: null, 3: null });
    fixture.detectChanges();
    return fixture;
  }

  it('counts only the readings inside the tank height', () => {
    const component = create().componentInstance;

    expect(component.measuredCount()).toBe(1);
    expect(component.allMeasured()).toBe(false);
    expect(component.isHeightInvalid(TANKS[1])).toBe(true);
    expect(component.isHeightInvalid(TANKS[0])).toBe(false);
  });

  it('shows the height with a comma and the filling percentage', () => {
    const component = create().componentInstance;

    expect(component.heightText(1)).toBe('100');
    expect(component.heightText(3)).toBe('');
    expect(component.fillPercent(TANKS[0])).toBe(`${formatGallons(40, 1)} %`);
    expect(component.fillPercent(TANKS[1])).toBe('—');
  });

  it('emits the parsed height of the edited tank', () => {
    const component = create().componentInstance;
    const emitted = vi.fn();
    component.heightChanged.subscribe(emitted);

    component.onHeightInput(TANKS[0], { target: { value: '123,5' } } as unknown as Event);

    expect(emitted).toHaveBeenCalledWith({ tankId: 1, height: 123.5 });
  });

  it('pastes the heights from the focused row down', () => {
    const component = create().componentInstance;
    const emitted = vi.fn();
    component.heightsPasted.subscribe(emitted);
    const paste = {
      clipboardData: { getData: () => '100\n110\n120' },
      preventDefault: vi.fn()
    } as unknown as ClipboardEvent;

    component.onHeightsPaste(paste, 0);

    expect(paste.preventDefault).toHaveBeenCalled();
    expect(emitted).toHaveBeenCalledWith({ 1: 100, 2: 110, 3: 120 });
  });

  it('ignores a single value pasted over the column', () => {
    const component = create().componentInstance;
    const emitted = vi.fn();
    component.heightsPasted.subscribe(emitted);
    const paste = {
      clipboardData: { getData: () => '100' },
      preventDefault: vi.fn()
    } as unknown as ClipboardEvent;

    component.onHeightsPaste(paste, 0);

    expect(paste.preventDefault).not.toHaveBeenCalled();
    expect(emitted).not.toHaveBeenCalled();
  });

  it('does not emit changes on a completed step', () => {
    const fixture = create();
    fixture.componentRef.setInput('editable', false);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const emitted = vi.fn();
    component.heightChanged.subscribe(emitted);

    component.onHeightInput(TANKS[0], { target: { value: '150' } } as unknown as Event);

    expect(emitted).not.toHaveBeenCalled();
  });
});
