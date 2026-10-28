import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { ProductStockResponse } from '../../../../shared/models/dtos/product-stock.dto';
import { StockMovementDialogComponent } from './stock-movement-dialog.component';

const PRODUCT: ProductStockResponse = {
  productId: 7,
  productCode: '80216',
  productName: 'Terpel Celerity 4T',
  category: 'LUBRICANTE',
  active: true,
  office: { opening: 10, initial: 0, entries: 0, transfers: 0, adjustments: 0, sold: 2, closing: 10 },
  counter: { opening: 6, initial: 0, entries: 0, transfers: 0, adjustments: 0, sold: 4, closing: 2 },
  status: 'OK'
};

describe('StockMovementDialogComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [StockMovementDialogComponent] }).compileComponents();
  });

  function create() {
    const fixture = TestBed.createComponent(StockMovementDialogComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('emits the quantity of a valid entry', () => {
    const component = create().componentInstance;
    const emitted = vi.fn();
    component.confirmed.subscribe(emitted);
    component.context.set({ mode: 'entry', product: PRODUCT });
    component.entryQuantity.set(3);

    component.confirm();

    expect(emitted).toHaveBeenCalledWith({
      context: { mode: 'entry', product: PRODUCT },
      values: { officeQuantity: null, counterQuantity: null, quantity: 3 }
    });
  });

  it('rejects an entry that is not a positive whole number', () => {
    const component = create().componentInstance;
    const emitted = vi.fn();
    component.confirmed.subscribe(emitted);
    component.context.set({ mode: 'entry', product: PRODUCT });
    component.entryQuantity.set(1.5);

    component.confirm();

    expect(component.validationError()).toBe('La cantidad debe ser un número entero mayor que cero.');
    expect(emitted).not.toHaveBeenCalled();
  });

  it('emits both locations of a valid initial stock', () => {
    const component = create().componentInstance;
    const emitted = vi.fn();
    component.confirmed.subscribe(emitted);
    component.context.set({ mode: 'initial', product: PRODUCT });
    component.initialOffice.set(5);
    component.initialCounter.set(2);

    component.confirm();

    expect(emitted).toHaveBeenCalledWith({
      context: { mode: 'initial', product: PRODUCT },
      values: { officeQuantity: 5, counterQuantity: 2, quantity: null }
    });
  });

  it('rejects a negative initial stock', () => {
    const component = create().componentInstance;
    const emitted = vi.fn();
    component.confirmed.subscribe(emitted);
    component.context.set({ mode: 'initial', product: PRODUCT });
    component.initialOffice.set(-1);
    component.initialCounter.set(2);

    component.confirm();

    expect(component.validationError()).toBe('Las existencias deben ser números enteros mayores o iguales a cero.');
    expect(emitted).not.toHaveBeenCalled();
  });
});
