import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ProductsService } from './products.service';
import { ProductRequest } from '../../shared/models/dtos/product.dto';

describe('ProductsService', () => {
  let service: ProductsService;
  let http: HttpTestingController;
  const url = 'http://localhost:8080/api/v1/inventory/products';

  const request: ProductRequest = {
    code: '80216',
    name: 'Terpel Celerity 4T',
    category: 'LUBRICANTE',
    price: 28500,
    cost: null,
    active: true
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ProductsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists every product without filters', () => {
    service.list().subscribe();
    const call = http.expectOne(url);
    expect(call.request.method).toBe('GET');
    expect(call.request.params.keys()).toEqual([]);
    call.flush([]);
  });

  it('sends only the filters with a value', () => {
    service.list({ category: 'ADITIVO', active: false }).subscribe();
    http.expectOne(`${url}?category=ADITIVO&active=false`).flush([]);

    service.list({ category: null, active: null }).subscribe();
    http.expectOne(url).flush([]);
  });

  it('creates a product', () => {
    service.create(request).subscribe();
    const call = http.expectOne(url);
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(request);
    call.flush({ id: 1, ...request, marginPercent: null });
  });

  it('updates a product by id', () => {
    service.update(7, request).subscribe();
    const call = http.expectOne(`${url}/7`);
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual(request);
    call.flush({ id: 7, ...request, marginPercent: 23.1 });
  });
});
