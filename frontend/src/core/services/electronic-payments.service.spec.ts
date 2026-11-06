import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ElectronicPaymentsService } from './electronic-payments.service';

describe('ElectronicPaymentsService', () => {
  let service: ElectronicPaymentsService;
  let http: HttpTestingController;
  const sales = 'http://localhost:8080/api/v1/sales';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ElectronicPaymentsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('asks for the status of the step by date', () => {
    service.getStatus('2026-08-05').subscribe();
    const call = http.expectOne(`${sales}/electronic-payments/2026-08-05`);
    expect(call.request.method).toBe('GET');
    call.flush({});
  });

  it('saves the six voucher amounts of a vendor', () => {
    const body = { mastercard: 1, visa: 2, amex: 3, mastercardDawn: 4, visaDawn: 5, amexDawn: 6 };
    service.saveVouchers('2026-08-05', 'w1', body).subscribe();
    const call = http.expectOne(`${sales}/electronic-payments/2026-08-05/vendors/w1/vouchers`);
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual(body);
    call.flush({});
  });

  it('uploads the Redeban CSV as multipart field "file"', () => {
    const file = new File(['a,b'], 'redeban.csv', { type: 'text/csv' });
    service.importRedeban('2026-08-05', file).subscribe();
    const call = http.expectOne(`${sales}/reconciliation/qr/2026-08-05/redeban`);
    expect(call.request.method).toBe('POST');
    expect(call.request.body instanceof FormData).toBe(true);
    const sent = (call.request.body as FormData).get('file') as File;
    expect(sent.name).toBe('redeban.csv');
    call.flush({});
  });

  it('saves the Puntos Colombia total', () => {
    service.savePoints('2026-08-05', 41380).subscribe();
    const call = http.expectOne(`${sales}/reconciliation/points/2026-08-05`);
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual({ total: 41380 });
    call.flush({});
  });

  it('reads the WhatsApp text of a vendor', () => {
    let text = '';
    service.getQrSummary('2026-08-05', 'w1').subscribe(response => (text = response.text));
    const call = http.expectOne(`${sales}/reconciliation/qr/2026-08-05/summary/w1`);
    expect(call.request.method).toBe('GET');
    call.flush({ text: '*Cierre QR 05/08/2026*' });
    expect(text).toBe('*Cierre QR 05/08/2026*');
  });
});
