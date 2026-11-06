import {
  AccountingReport,
  CredibancoDayResult,
  ElectronicPaymentsStatus,
  ElectronicVendorRow,
  PeriodTotals,
  PointsResult,
  QrReconciliationResult
} from '../models/dtos/electronic-payments.dto';
import {
  assignmentReason,
  buildStepSummary,
  countMatchingVendors,
  credibancoTone,
  formatAmount,
  formatSignedCop,
  formatTime,
  parseAmount,
  pointsTone,
  qrTone,
  validateRedebanFile,
  vendorLabel,
  vendorMatches,
  voucherValues
} from './electronic-payments.helpers';

const NO_PERIOD: PeriodTotals = { mastercard: 0, visa: 0, amex: 0, qr: 0, totalEds: 0, source: 'NONE' };
const NO_ACCOUNTING: AccountingReport = { amex: 0, mastercard: 0, visa: 0, qr: null, total: null };

function row(workerId: string, name: string, shiftCode: string, extra: Partial<ElectronicVendorRow> = {}): ElectronicVendorRow {
  return {
    workerId,
    workerName: name,
    shiftCode,
    mastercard: 0,
    visa: 0,
    amex: 0,
    mastercardDawn: 0,
    visaDawn: 0,
    amexDawn: 0,
    planillaCredibanco: 0,
    planillaQr: 0,
    planillaRedemptions: 0,
    ...extra
  };
}

function credibanco(overrides: Partial<CredibancoDayResult> = {}): CredibancoDayResult {
  return {
    date: '2026-08-05',
    vendors: [],
    allMatch: false,
    franchises: { amex: 0, mastercard: 0, visa: 0, amexDawn: 0, mastercardDawn: 0, visaDawn: 0, qr: 0 },
    qrAdded: false,
    totalEds: 0,
    accounting: NO_ACCOUNTING,
    monthToDate: NO_PERIOD,
    previousMonth: NO_PERIOD,
    commissionRate: 0,
    commissionBase: 0,
    commission: 0,
    ...overrides
  };
}

function qr(overrides: Partial<QrReconciliationResult> = {}): QrReconciliationResult {
  return {
    date: '2026-08-05',
    redebanLoaded: false,
    redebanTotal: 0,
    planillaTotal: 0,
    qrTotal: 0,
    difference: 0,
    matchesDay: false,
    unassignedTotal: 0,
    perVendorReliable: false,
    byStatus: {},
    unassigned: [],
    vendors: [],
    ...overrides
  };
}

function points(overrides: Partial<PointsResult> = {}): PointsResult {
  return {
    date: '2026-08-05',
    declared: 0,
    pointsTotal: null,
    difference: null,
    status: 'COMPLETED_ZERO',
    source: null,
    vendors: [],
    ...overrides
  };
}

/** Dos vendedores: Carlos cuadra en todo; Ana tiene 19.760 de diferencia en Credibanco. */
function status(overrides: Partial<ElectronicPaymentsStatus> = {}): ElectronicPaymentsStatus {
  return {
    date: '2026-08-05',
    canComplete: false,
    blockers: [],
    vendors: [
      row('w1', 'Carlos', 'DIA', { planillaCredibanco: 1354436, planillaQr: 162606 }),
      row('w2', 'Ana', 'NOCHE', { planillaCredibanco: 2019760, planillaQr: 377902 })
    ],
    credibanco: credibanco({
      vendors: [
        { workerId: 'w1', workerName: 'Carlos', shiftCode: 'DIA', vouchersTotal: 1354436, planillaCredibanco: 1354436, difference: 0, matches: true },
        { workerId: 'w2', workerName: 'Ana', shiftCode: 'NOCHE', vouchersTotal: 2039520, planillaCredibanco: 2019760, difference: 19760, matches: false }
      ]
    }),
    qr: qr({
      redebanLoaded: true,
      redebanTotal: 540508,
      planillaTotal: 540508,
      qrTotal: 540508,
      matchesDay: true,
      perVendorReliable: true,
      vendors: [
        { workerId: 'w1', workerName: 'Carlos', shiftCode: 'DIA', redeban: 162606, planilla: 162606, difference: 0, matches: true },
        { workerId: 'w2', workerName: 'Ana', shiftCode: 'NOCHE', redeban: 377902, planilla: 377902, difference: 0, matches: true }
      ]
    }),
    points: points(),
    ...overrides
  };
}

describe('electronic payments helpers', () => {
  describe('parseAmount', () => {
    it('reads Colombian formats', () => {
      expect(parseAmount('1.354.436')).toBe(1354436);
      expect(parseAmount('1354436')).toBe(1354436);
      expect(parseAmount('$ 2.019.760')).toBe(2019760);
      expect(parseAmount('1354436,5')).toBe(1354436.5);
      expect(parseAmount('1.234,56')).toBe(1234.56);
      expect(parseAmount('12.5')).toBe(12.5);
    });

    it('treats a blank field as zero', () => {
      expect(parseAmount('')).toBe(0);
      expect(parseAmount('   ')).toBe(0);
    });

    it('rejects letters, negatives, ambiguous separators and amounts that are too large', () => {
      expect(parseAmount('abc')).toBeNull();
      expect(parseAmount('-5')).toBeNull();
      expect(parseAmount('1,234,567')).toBeNull();
      expect(parseAmount('12,345')).toBeNull();
      expect(parseAmount('1.2.3')).toBeNull();
      expect(parseAmount('1000000000000')).toBeNull();
    });
  });

  describe('formatAmount', () => {
    it('shows thousands dots, keeps real decimals and leaves zero blank', () => {
      expect(formatAmount(0)).toBe('');
      expect(formatAmount(1354436)).toBe('1.354.436');
      expect(formatAmount(1234.5)).toBe('1.234,50');
    });

    it('round-trips with parseAmount', () => {
      for (const value of [1, 999, 1000, 41380, 1354436, 1234.56]) {
        expect(parseAmount(formatAmount(value))).toBe(value);
      }
    });
  });

  it('formats differences with their sign', () => {
    expect(formatSignedCop(0)).toBe('$0');
    expect(formatSignedCop(531)).toBe('+$531');
    expect(formatSignedCop(-531)).toBe('-$531');
    expect(formatSignedCop(19760)).toBe('+$19.760');
  });

  it('builds the six amounts sent to the server from a row', () => {
    const body = voucherValues(row('w1', 'Carlos', 'DIA', { mastercard: 10, visa: 20, amex: 30, visaDawn: 5 }));
    expect(body).toEqual({ mastercard: 10, visa: 20, amex: 30, mastercardDawn: 0, visaDawn: 5, amexDawn: 0 });
  });

  describe('tab tones', () => {
    it('Credibanco: amber with no vendors, green when all match, red otherwise', () => {
      expect(credibancoTone(credibanco())).toBe('aviso');
      expect(credibancoTone(status().credibanco)).toBe('error');
      const matching = credibanco({ allMatch: true, vendors: [status().credibanco.vendors[0]] });
      expect(credibancoTone(matching)).toBe('ok');
    });

    it('QR: nothing to compare is fine, a missing file is pending, differences are red', () => {
      expect(qrTone(qr())).toBe('ok');
      expect(qrTone(qr({ planillaTotal: 100 }))).toBe('aviso');
      expect(qrTone(status().qr)).toBe('ok');
      expect(qrTone({ ...status().qr, matchesDay: false })).toBe('error');
      expect(qrTone({ ...status().qr, perVendorReliable: false })).toBe('aviso');
      const oneOff = { ...status().qr, vendors: [{ ...status().qr.vendors[0], matches: false }] };
      expect(qrTone(oneOff)).toBe('error');
    });

    it('Puntos: zero and match are green, pending is amber, difference is red', () => {
      expect(pointsTone(points({ status: 'COMPLETED_ZERO' }))).toBe('ok');
      expect(pointsTone(points({ status: 'MATCH' }))).toBe('ok');
      expect(pointsTone(points({ status: 'PENDING_TOTAL' }))).toBe('aviso');
      expect(pointsTone(points({ status: 'DIFFERENCE' }))).toBe('error');
    });
  });

  describe('which vendors match', () => {
    it('needs Credibanco and QR to match for the same vendor', () => {
      const current = status();
      expect(vendorMatches(current, 'w1')).toBe(true);
      expect(vendorMatches(current, 'w2')).toBe(false);
      expect(vendorMatches(current, 'desconocido')).toBe(false);
      expect(countMatchingVendors(current)).toBe(1);
    });

    it('without the Redeban file only a vendor with no QR in the planilla matches', () => {
      const withoutFile = status({ qr: qr() });
      expect(vendorMatches(withoutFile, 'w1')).toBe(false);
      const noQrDeclared = status({
        qr: qr(),
        vendors: [row('w1', 'Carlos', 'DIA'), row('w2', 'Ana', 'NOCHE')]
      });
      expect(vendorMatches(noQrDeclared, 'w1')).toBe(true);
    });
  });

  it('labels vendors with their shift and has a fallback name', () => {
    expect(vendorLabel('Carlos', 'DIA')).toBe('Carlos (DIA)');
    expect(vendorLabel('Carlos', null)).toBe('Carlos');
    expect(vendorLabel(null, 'DIA')).toBe('Vendedor sin nombre (DIA)');
  });

  it('explains why a Redeban row was not assigned', () => {
    expect(assignmentReason('AMBIGUOUS')).toBe('Varios vendedores a esa hora');
    expect(assignmentReason('NO_SHIFT')).toBe('Ningún turno cubre esa hora');
    expect(assignmentReason('NO_TIME')).toBe('Sin hora en el archivo');
  });

  it('shows the time without seconds', () => {
    expect(formatTime('13:45:10')).toBe('13:45');
    expect(formatTime(null)).toBe('—');
  });

  describe('validateRedebanFile', () => {
    it('accepts a CSV', () => {
      expect(validateRedebanFile({ name: 'Redeban-05.CSV', size: 1200 })).toBeNull();
    });

    it('rejects other types, empty files and files over 25 MB', () => {
      expect(validateRedebanFile({ name: 'redeban.xlsx', size: 1200 })).toContain('CSV');
      expect(validateRedebanFile({ name: 'redeban.csv', size: 0 })).toContain('vacío');
      expect(validateRedebanFile({ name: 'redeban.csv', size: 26 * 1024 * 1024 })).toContain('25 MB');
    });
  });

  describe('buildStepSummary', () => {
    it('lists each method and each vendor with their state', () => {
      const text = buildStepSummary(status());
      expect(text).toContain('*Medios electrónicos 05/08/2026*');
      expect(text).toContain('Credibanco: No cuadra');
      expect(text).toContain('- Carlos (DIA): Cuadra');
      expect(text).toContain('- Ana (NOCHE): No cuadra (comprobantes $2.039.520, planilla $2.019.760)');
      expect(text).toContain('QR: Cuadra (Redeban $540.508, planillas $540.508)');
      expect(text).toContain('Puntos Colombia: sin redenciones');
      expect(text).toContain('Paso 3: pendiente.');
    });

    it('says what is missing when the Redeban file or the points total are not loaded', () => {
      const text = buildStepSummary(
        status({
          qr: qr({ planillaTotal: 540508 }),
          points: points({ status: 'PENDING_TOTAL', declared: 41380 })
        })
      );
      expect(text).toContain('QR: pendiente de cargar el archivo de Redeban');
      expect(text).toContain('Puntos Colombia: pendiente el total (redenciones $41.380)');
    });

    it('declares the step done when everything matches', () => {
      const text = buildStepSummary(status({ canComplete: true, points: points({ status: 'MATCH', declared: 41380, pointsTotal: 41380, difference: 0 }) }));
      expect(text).toContain('Puntos Colombia: Cuadra ($41.380)');
      expect(text).toContain('Paso 3: todo cuadra.');
    });
  });
});
