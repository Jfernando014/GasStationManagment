import { PDFDocument } from 'pdf-lib';

import { AdministrativeExpenseDto } from '../../../shared/models/dtos/administrative-expense.dto';
import { buildReimbursementPdf } from './administrative-expenses-pdf';

const SAMPLE_EXPENSES: AdministrativeExpenseDto[] = [
  {
    id: 1,
    date: '2026-08-01',
    amount: 6000,
    name: 'Ofimatic@.NET',
    documentNumber: '25275622-2',
    description: '20 IMPRESIONES B/N',
    createdAt: '2026-08-01T10:00:00Z'
  },
  {
    id: 2,
    date: '2026-08-01',
    amount: 18350,
    name: 'Servientrega S.A',
    documentNumber: '860512330-3',
    description: 'Envío documentos Bogotá',
    createdAt: '2026-08-01T12:00:00Z'
  }
];

describe('administrative-expenses-pdf', () => {
  it('builds a valid PDF reimbursement sheet for the selected period', async () => {
    const bytes = await buildReimbursementPdf({
      month: '2026-08',
      periodLabel: 'Quincena 1 (1 al 15)',
      isFortnight: true,
      expenses: SAMPLE_EXPENSES,
      total: 24350,
      generatedAt: new Date('2026-08-15T18:00:00')
    });

    const doc = await PDFDocument.load(bytes);
    expect(bytes.byteLength).toBeGreaterThan(500);
    expect(doc.getPageCount()).toBe(1);
  });
});
