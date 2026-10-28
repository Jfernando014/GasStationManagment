import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { ProductStockService } from '../../../../core/services/product-stock.service';
import { ProductsService } from '../../../../core/services/products.service';
import { ApiError } from '../../../../shared/models/api-error.model';
import { ProductResponse } from '../../../../shared/models/dtos/product.dto';
import { ProductStockResponse } from '../../../../shared/models/dtos/product-stock.dto';
import { ProductsPageComponent } from './products-page.component';

const PRODUCTS: ProductResponse[] = [
  {
    id: 1,
    code: '80216',
    name: 'Terpel Celerity 4T',
    category: 'LUBRICANTE',
    price: 28500,
    cost: 21924,
    active: true,
    marginPercent: 23.1
  },
  {
    id: 2,
    code: '75089',
    name: 'Super Concentrado 4x1',
    category: 'ADITIVO',
    price: 7200,
    cost: null,
    active: false,
    marginPercent: null
  }
];

const STOCK: ProductStockResponse[] = [
  {
    productId: 1,
    productCode: '80216',
    productName: 'Terpel Celerity 4T',
    category: 'LUBRICANTE',
    active: true,
    office: { opening: 10, initial: 0, entries: 4, transfers: -2, adjustments: 0, sold: 3, closing: 9 },
    counter: { opening: 4, initial: 0, entries: 0, transfers: 2, adjustments: 0, sold: 5, closing: 1 },
    status: 'OK'
  }
];

describe('ProductsPageComponent', () => {
  let productsService: { list: ReturnType<typeof vi.fn>; create: ReturnType<typeof vi.fn>; update: ReturnType<typeof vi.fn> };
  let stockService: { listByJournalDate: ReturnType<typeof vi.fn> };

  function create() {
    const fixture = TestBed.createComponent(ProductsPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(async () => {
    productsService = {
      list: vi.fn(() => of(PRODUCTS)),
      create: vi.fn(),
      update: vi.fn()
    };
    stockService = { listByJournalDate: vi.fn(() => of(STOCK)) };

    await TestBed.configureTestingModule({
      imports: [ProductsPageComponent],
      providers: [
        provideRouter([]),
        { provide: ProductsService, useValue: productsService },
        { provide: ProductStockService, useValue: stockService }
      ]
    }).compileComponents();
  });

  it('loads the catalog and the stock of today', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    expect(productsService.list).toHaveBeenCalledTimes(1);
    expect(component.products()).toHaveLength(2);
    expect(component.stockOf(component.products()[0])?.counter.closing).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Terpel Celerity 4T');
  });

  it('filters the catalog by name or code without calling the backend again', () => {
    const component = create().componentInstance;

    component.search.set('75089');
    expect(component.visibleProducts()).toHaveLength(1);
    expect(component.visibleProducts()[0].name).toBe('Super Concentrado 4x1');
    expect(productsService.list).toHaveBeenCalledTimes(1);
  });

  it('keeps only the products with cost in the margins tab', () => {
    const component = create().componentInstance;

    expect(component.margins()).toHaveLength(1);
    expect(component.averageMargin()).toBe(23.1);
    expect(component.bestMargin()?.code).toBe('80216');
    expect(component.withoutCost()).toBe(1);
  });

  it('filters by category and status in the browser, without asking the backend again', () => {
    const component = create().componentInstance;

    component.onCategoryChange('ADITIVO');
    expect(component.visibleProducts()).toHaveLength(1);
    expect(component.visibleProducts()[0].category).toBe('ADITIVO');

    component.onActiveChange('active');
    expect(component.visibleProducts()).toHaveLength(0);

    component.onCategoryChange('');
    expect(component.visibleProducts()).toHaveLength(1);
    expect(component.visibleProducts()[0].active).toBe(true);

    expect(productsService.list).toHaveBeenCalledTimes(1);
  });

  it('does not send an invalid product and marks the fields', () => {
    const fixture = create();
    const component = fixture.componentInstance;

    component.openCreate();
    component.draft.update(draft => ({ ...draft, code: '', name: '', price: null }));
    component.save();
    fixture.detectChanges();

    expect(productsService.create).not.toHaveBeenCalled();
    expect(component.formError()).toBe('Revisa los datos marcados para guardar el producto.');
    expect(fixture.nativeElement.textContent).toContain('El código es obligatorio');
    expect(fixture.nativeElement.textContent).toContain('El precio debe ser mayor que cero');
  });

  it('shows the backend field errors next to the field', () => {
    const fixture = create();
    const component = fixture.componentInstance;
    productsService.create.mockReturnValue(
      throwError(() => new ApiError(400, { message: 'Invalid data', fields: [{ field: 'code', message: 'The code already exists' }] }))
    );

    component.openCreate();
    component.draft.set({ code: '80216', name: 'Repetido', category: 'LUBRICANTE', price: 1000, cost: null, active: true });
    component.save();
    fixture.detectChanges();

    expect(component.fieldError('code')).toBe('The code already exists');
    expect(fixture.nativeElement.textContent).toContain('The code already exists');
    expect(component.formOpen()).toBe(true);
  });

  it('adds the created product to the catalog and closes the form', () => {
    const fixture = create();
    const component = fixture.componentInstance;
    const created: ProductResponse = {
      id: 3,
      code: '90001',
      name: 'Aditivo nuevo',
      category: 'ADITIVO',
      price: 5000,
      cost: 3000,
      active: true,
      marginPercent: 40
    };
    productsService.create.mockReturnValue(of(created));

    component.openCreate();
    component.draft.set({ code: '90001', name: 'Aditivo nuevo', category: 'ADITIVO', price: 5000, cost: 3000, active: true });
    component.save();

    expect(productsService.create).toHaveBeenCalledWith({
      code: '90001',
      name: 'Aditivo nuevo',
      category: 'ADITIVO',
      price: 5000,
      cost: 3000,
      active: true
    });
    expect(component.products()).toContainEqual(created);
    expect(component.formOpen()).toBe(false);
  });

  it('shows zeros for a product created in this session instead of a dash', () => {
    const fixture = create();
    const component = fixture.componentInstance;
    const created: ProductResponse = {
      id: 4,
      code: '90002',
      name: 'Refrigerante nuevo',
      category: 'REFRIGERANTE',
      price: 9000,
      cost: null,
      active: true,
      marginPercent: null
    };
    productsService.create.mockReturnValue(of(created));

    component.openCreate();
    component.draft.set({ code: '90002', name: 'Refrigerante nuevo', category: 'REFRIGERANTE', price: 9000, cost: null, active: true });
    component.save();
    fixture.detectChanges();

    const seeded = component.stockOf(created);
    expect(seeded?.office.closing).toBe(0);
    expect(seeded?.counter.closing).toBe(0);
    expect(seeded?.status).toBe('OK');
  });

  it('keeps the catalog when the stock cannot be loaded', () => {
    stockService.listByJournalDate.mockReturnValue(throwError(() => new ApiError(404, { detail: 'Sin jornada' })));
    const fixture = create();
    const component = fixture.componentInstance;

    expect(component.stockError()).toBe(true);
    expect(component.products()).toHaveLength(2);
    expect(component.stockOf(component.products()[0])).toBeUndefined();
  });
});
