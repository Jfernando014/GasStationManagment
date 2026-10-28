import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { ProductStockResponse } from '../../../../shared/models/dtos/product-stock.dto';
import { LubricantStockTableComponent } from './lubricant-stock-table.component';

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

describe('LubricantStockTableComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [LubricantStockTableComponent] }).compileComponents();
  });

  function create(pendingTransfers: Record<number, number | null> = {}) {
    const fixture = TestBed.createComponent(LubricantStockTableComponent);
    fixture.componentRef.setInput('products', [PRODUCT]);
    fixture.componentRef.setInput('totalProducts', 4);
    fixture.componentRef.setInput('filter', 'movimiento');
    fixture.componentRef.setInput('search', '');
    fixture.componentRef.setInput('pendingTransfers', pendingTransfers);
    fixture.componentRef.setInput('editable', true);
    fixture.detectChanges();
    return fixture;
  }

  it('rejects a transfer larger than the office stock', () => {
    const component = create({ 7: 40 }).componentInstance;

    expect(component.transferInvalid(PRODUCT)).toBe(true);
  });

  it('accepts a transfer within the office stock and projects the counter', () => {
    const component = create({ 7: 3 }).componentInstance;

    expect(component.transferInvalid(PRODUCT)).toBe(false);
    expect(component.transferOf(7)).toBe(3);
    expect(component.existingTotal(PRODUCT)).toBe(12);
    expect(component.counterAfterTransfer(PRODUCT)).toBe(5);
  });

  it('emits the typed quantity and null when the field is cleared', () => {
    const component = create().componentInstance;
    const emitted = vi.fn();
    component.transferChanged.subscribe(emitted);

    component.onTransferInput(PRODUCT, { target: { value: '3' } } as unknown as Event);
    component.onTransferInput(PRODUCT, { target: { value: '' } } as unknown as Event);

    expect(emitted).toHaveBeenNthCalledWith(1, { productId: 7, quantity: 3 });
    expect(emitted).toHaveBeenNthCalledWith(2, { productId: 7, quantity: null });
  });

  it('shows the count of visible products over the total', () => {
    const fixture = create();
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Mostrando 1 de 4 productos');
  });
});
