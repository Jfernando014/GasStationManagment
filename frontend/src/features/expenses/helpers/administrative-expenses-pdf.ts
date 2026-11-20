import { PDFDocument, PDFFont, PDFPage, StandardFonts, rgb } from 'pdf-lib';

import { AdministrativeExpenseDto } from '../../../shared/models/dtos/administrative-expense.dto';
import { formatCop } from '../../../shared/helpers/format.helpers';

export const PETTY_CASH_FUND_COP = 500_000;

export interface ReimbursementPdfInput {
  month: string;
  periodLabel: string;
  isFortnight: boolean;
  expenses: readonly AdministrativeExpenseDto[];
  total: number;
  generatedAt?: Date;
}

const A4: [number, number] = [595.28, 841.89];
const MARGIN = 40;
const ROW_HEIGHT = 24;
const INK = rgb(0.06, 0.09, 0.16);
const MUTED = rgb(0.39, 0.45, 0.55);
const LINE = rgb(0.89, 0.91, 0.94);
const BAND = rgb(0.95, 0.96, 0.98);
const TITLE = rgb(0.04, 0.1, 0.17);

function safe(text: string): string {
  return text
    .replace(/[\u2212\u2013\u2014]/g, '-')
    .replace(/[\u00A0\u202F]/g, ' ')
    .replace(/[\u2018\u2019]/g, "'")
    .replace(/[\u201C\u201D]/g, '"')
    .replace(/[^\x20-\x7E\xA1-\xFF]/g, '?');
}

function fit(text: string, font: PDFFont, size: number, maxWidth: number): string {
  let output = safe(text);
  if (font.widthOfTextAtSize(output, size) <= maxWidth) return output;
  while (output.length > 1 && font.widthOfTextAtSize(`${output}...`, size) > maxWidth) {
    output = output.slice(0, -1);
  }
  return `${output}...`;
}

function shortDate(isoDate: string): string {
  const [, month, day] = isoDate.split('-');
  return day && month ? `${day}/${month}` : isoDate;
}

/** Builds the petty cash reimbursement sheet PDF ("Planilla de reembolso") for the selected period. */
export async function buildReimbursementPdf(input: ReimbursementPdfInput): Promise<Uint8Array> {
  const document = await PDFDocument.create();
  const regular = await document.embedFont(StandardFonts.Helvetica);
  const bold = await document.embedFont(StandardFonts.HelveticaBold);
  const generated = input.generatedAt ?? new Date();
  const stamp = `${String(generated.getDate()).padStart(2, '0')}/${String(generated.getMonth() + 1).padStart(2, '0')}/${generated.getFullYear()} ${String(generated.getHours()).padStart(2, '0')}:${String(generated.getMinutes()).padStart(2, '0')}`;

  const [width, height] = A4;
  const contentWidth = width - MARGIN * 2;
  let page: PDFPage = document.addPage(A4);
  let y = height - 46;

  const drawHeader = () => {
    page.drawText(safe('EDS BELLAVISTA · CENTRO DE COSTO 761'), {
      x: MARGIN,
      y,
      size: 9,
      font: bold,
      color: MUTED
    });
    y -= 16;
    page.drawText(safe('Planilla de Reembolso - Gastos Administrativos'), {
      x: MARGIN,
      y,
      size: 16,
      font: bold,
      color: TITLE
    });
    y -= 16;
    page.drawText(safe(`Periodo: ${input.periodLabel} (${input.month}) · Generado: ${stamp}`), {
      x: MARGIN,
      y,
      size: 9.5,
      font: regular,
      color: MUTED
    });
    y -= 18;
    page.drawRectangle({ x: MARGIN, y, width: contentWidth, height: 22, color: BAND });
    page.drawText('Fecha', { x: MARGIN + 8, y: y + 7, size: 8.5, font: bold, color: MUTED });
    page.drawText('Nombre / NIT o CC', { x: MARGIN + 58, y: y + 7, size: 8.5, font: bold, color: MUTED });
    page.drawText('Concepto', { x: MARGIN + 235, y: y + 7, size: 8.5, font: bold, color: MUTED });
    const valTitle = 'Valor';
    const valW = bold.widthOfTextAtSize(valTitle, 8.5);
    page.drawText(valTitle, { x: width - MARGIN - 8 - valW, y: y + 7, size: 8.5, font: bold, color: MUTED });
    y -= 4;
  };

  drawHeader();

  for (const expense of input.expenses) {
    if (y - ROW_HEIGHT < MARGIN + 90) {
      page = document.addPage(A4);
      y = height - 46;
      drawHeader();
    }
    const rowBottom = y - ROW_HEIGHT;
    page.drawLine({
      start: { x: MARGIN, y: rowBottom },
      end: { x: width - MARGIN, y: rowBottom },
      thickness: 0.6,
      color: LINE
    });
    const supplier = expense.name?.trim() || 'Gasto administrativo';
    const beneficiary = expense.documentNumber ? `${supplier} (${expense.documentNumber})` : supplier;
    const amountText = safe(formatCop(expense.amount));
    const amountWidth = bold.widthOfTextAtSize(amountText, 9.5);

    page.drawText(shortDate(expense.date), { x: MARGIN + 8, y: rowBottom + 8, size: 9, font: regular, color: INK });
    page.drawText(fit(beneficiary, bold, 9, 168), { x: MARGIN + 58, y: rowBottom + 8, size: 9, font: bold, color: INK });
    page.drawText(fit(expense.description, regular, 9, 185), { x: MARGIN + 235, y: rowBottom + 8, size: 9, font: regular, color: INK });
    page.drawText(amountText, { x: width - MARGIN - 8 - amountWidth, y: rowBottom + 8, size: 9.5, font: bold, color: INK });
    y = rowBottom;
  }

  y -= 14;
  const summaryRows: [string, string, boolean][] = [
    ['Comprobantes incluidos', String(input.expenses.length), false],
    ['Total a reembolsar', safe(formatCop(input.total)), true]
  ];
  if (input.isFortnight) {
    const available = Math.max(PETTY_CASH_FUND_COP - input.total, 0);
    summaryRows.push(['Fondo fijo caja menor', safe(formatCop(PETTY_CASH_FUND_COP)), false]);
    summaryRows.push(['Disponible en caja menor', safe(formatCop(available)), false]);
  }

  for (const [label, value, strong] of summaryRows) {
    const rowBottom = y - 22;
    if (strong) {
      page.drawRectangle({ x: MARGIN, y: rowBottom, width: contentWidth, height: 22, color: BAND });
    }
    const font = strong ? bold : regular;
    const valW = font.widthOfTextAtSize(value, 10);
    page.drawText(safe(label), { x: MARGIN + 8, y: rowBottom + 7, size: 9.5, font, color: INK });
    page.drawText(value, { x: width - MARGIN - 8 - valW, y: rowBottom + 7, size: 10, font, color: INK });
    y = rowBottom;
  }

  return document.save();
}
