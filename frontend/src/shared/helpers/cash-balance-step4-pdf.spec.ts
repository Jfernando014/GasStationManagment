import { PDFDocument } from 'pdf-lib';

import { buildFinalCashBalancePdf } from './cash-balance-step4-pdf';
import { CashBalanceTableDto } from '../models/dtos/cash-balance-step4.dto';

const table: CashBalanceTableDto = {
  date: '2026-08-01',
  journalConsecutive: 289,
  journalStatus: 'OPEN',
  savingsFund: {
    previousBalance: 2_750_000,
    contribution: 250_000,
    payments: 0,
    accumulatedBalance: 3_000_000
  },
  expenses: {
    items: [{ date: '2026-08-01', description: 'Papelería', amount: 20_000 }],
    total: 20_000
  },
  cashBalance: 9_364_000,
  dailyCash: 12_516_000,
  planillaCashBalance: 21_630_000,
  dominusAccumulatedBalance: 24_629_469,
  difference: -531
};

describe('cash-balance-step4-pdf', () => {
  it('preserves the source PDF and appends the cash-balance page', async () => {
    const source = await PDFDocument.create();
    source.addPage();
    source.addPage();

    const result = await buildFinalCashBalancePdf(await source.save(), {
      table,
      differenceStatus: 'PLANILLA_MAYOR',
      generatedAt: new Date('2026-08-01T10:00:00')
    });

    const finalDocument = await PDFDocument.load(result.bytes);
    expect(result.pageCount).toBe(3);
    expect(result.balancePages).toBe(1);
    expect(finalDocument.getPageCount()).toBe(3);
    expect(finalDocument.getPage(2).getSize().width).toBeGreaterThan(0);
  });

  it('continues the cash-balance details on another page when there are many expenses', async () => {
    const source = await PDFDocument.create();
    source.addPage();
    const expenses = Array.from({ length: 60 }, (_, index) => ({
      date: table.date,
      description: `Gasto administrativo ${index + 1}`,
      amount: 1_000
    }));

    const result = await buildFinalCashBalancePdf(await source.save(), {
      table: { ...table, expenses: { items: expenses, total: 60_000 } },
      differenceStatus: 'PLANILLA_MAYOR'
    });

    expect(result.balancePages).toBeGreaterThan(1);
    expect(result.pageCount).toBe(result.balancePages + 1);
  });
});
