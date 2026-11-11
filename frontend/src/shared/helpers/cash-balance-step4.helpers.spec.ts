import {
  cashBalanceTone,
  cashDifferenceLabel,
  hasPdfSignature,
  validateCashBalancePdf,
  validateFundPayment
} from './cash-balance-step4.helpers';

describe('cash-balance-step4.helpers', () => {
  it('labels backend difference states without recalculating the difference', () => {
    expect(cashDifferenceLabel('CUADRA')).toBe('Cuadra');
    expect(cashDifferenceLabel('PLANILLA_MAYOR')).toBe('Planilla mayor');
    expect(cashDifferenceLabel('PLANILLA_MENOR')).toBe('Planilla menor');
    expect(cashBalanceTone(null)).toBe('pending');
    expect(cashBalanceTone('CUADRA')).toBe('ok');
  });

  it('validates payment date, positive amount, and fund balance', () => {
    expect(validateFundPayment({ date: '', amountText: '10', balance: 100 })).toMatchObject({
      amount: null,
      error: 'Elige la fecha del pago.'
    });
    expect(validateFundPayment({ date: '2026-08-05', amountText: '0', balance: 100 }).amount).toBeNull();
    expect(validateFundPayment({ date: '2026-08-05', amountText: '1.000', balance: 100 }).error).toContain(
      'supera el saldo'
    );
    expect(validateFundPayment({ date: '2026-08-05', amountText: '50.000', balance: 100_000 })).toEqual({
      amount: 50_000,
      error: null
    });
  });

  it('accepts only PDF file names/types within the configured size and signature', () => {
    expect(validateCashBalancePdf({ name: 'document.txt', type: 'text/plain', size: 12 })).toContain('PDF');
    expect(validateCashBalancePdf({ name: 'empty.pdf', type: 'application/pdf', size: 0 })).toContain('vacío');
    expect(
      validateCashBalancePdf({ name: 'large.pdf', type: 'application/pdf', size: 25 * 1024 * 1024 + 1 })
    ).toContain('25 MB');
    expect(validateCashBalancePdf({ name: 'valid.pdf', type: 'application/pdf', size: 10 })).toBeNull();
    expect(hasPdfSignature(new Uint8Array([0x25, 0x50, 0x44, 0x46, 0x2d, 0x31]))).toBe(true);
    expect(hasPdfSignature(new Uint8Array([0x00, 0x01, 0x02, 0x03]))).toBe(false);
  });
});
