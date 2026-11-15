import { PDFDocument, PDFFont, PDFPage, StandardFonts, rgb } from 'pdf-lib';

import {
  CashBalanceTableDto,
  CashDifferenceStatus
} from '../models/dtos/cash-balance-step4.dto';
import { cashDifferenceLabel } from './cash-balance-step4.helpers';
import { formatSignedCop } from './electronic-payments.helpers';
import { formatCop, formatInputDate } from './format.helpers';

export interface CashBalancePdfInput {
  table: CashBalanceTableDto;
  differenceStatus: CashDifferenceStatus | null;
  generatedAt?: Date;
}

export interface CashBalancePdfResult {
  bytes: Uint8Array;
  pageCount: number;
  balancePages: number;
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

function money(value: number | null): string {
  return value === null ? 'Sin dato' : formatCop(value);
}

function fit(text: string, font: PDFFont, size: number, maxWidth: number): string {
  let output = safe(text);
  if (font.widthOfTextAtSize(output, size) <= maxWidth) return output;
  while (output.length > 1 && font.widthOfTextAtSize(`${output}...`, size) > maxWidth) {
    output = output.slice(0, -1);
  }
  return `${output}...`;
}

function twoDigits(value: number): string {
  return String(value).padStart(2, '0');
}

function pageSize(document: PDFDocument): [number, number] {
  const { width, height } = document.getPage(0).getSize();
  return [width, height];
}

export async function buildFinalCashBalancePdf(
  source: Uint8Array,
  input: CashBalancePdfInput
): Promise<CashBalancePdfResult> {
  const document = await PDFDocument.load(source);
  const originalPageCount = document.getPageCount();
  const [width, height] = originalPageCount > 0 ? pageSize(document) : A4;
  const regular = await document.embedFont(StandardFonts.Helvetica);
  const bold = await document.embedFont(StandardFonts.HelveticaBold);
  const contentWidth = width - MARGIN * 2;
  const generatedAt = input.generatedAt ?? new Date();
  let page: PDFPage = document.addPage([width, height]);
  let y = height - MARGIN;

  const newPage = (): void => {
    page = document.addPage([width, height]);
    y = height - MARGIN;
  };
  const ensureSpace = (needed: number): void => {
    if (y - needed < MARGIN) newPage();
  };
  const drawRow = (label: string, value: string, options: { strong?: boolean; shade?: boolean } = {}): void => {
    ensureSpace(ROW_HEIGHT);
    const font = options.strong ? bold : regular;
    if (options.shade) {
      page.drawRectangle({
        x: MARGIN,
        y: y - ROW_HEIGHT,
        width: contentWidth,
        height: ROW_HEIGHT,
        color: BAND
      });
    }
    const safeValue = safe(value);
    const valueWidth = font.widthOfTextAtSize(safeValue, 10.5);
    page.drawText(fit(label, font, 10.5, contentWidth - valueWidth - 28), {
      x: MARGIN + 8,
      y: y - 16,
      size: 10.5,
      font,
      color: INK
    });
    page.drawText(safeValue, {
      x: MARGIN + contentWidth - 8 - valueWidth,
      y: y - 16,
      size: 10.5,
      font,
      color: INK
    });
    page.drawLine({
      start: { x: MARGIN, y: y - ROW_HEIGHT },
      end: { x: MARGIN + contentWidth, y: y - ROW_HEIGHT },
      thickness: 0.5,
      color: LINE
    });
    y -= ROW_HEIGHT;
  };
  const drawSection = (title: string): void => {
    ensureSpace(ROW_HEIGHT * 2 + 14);
    y -= 14;
    page.drawText(safe(title), { x: MARGIN, y: y - 12, size: 11.5, font: bold, color: TITLE });
    y -= 20;
  };

  page.drawText('EDS Bellavista - Saldo en caja', {
    x: MARGIN,
    y: y - 18,
    size: 18,
    font: bold,
    color: TITLE
  });
  y -= 30;
  const timestamp = `${twoDigits(generatedAt.getDate())}/${twoDigits(generatedAt.getMonth() + 1)}/${generatedAt.getFullYear()} ${twoDigits(generatedAt.getHours())}:${twoDigits(generatedAt.getMinutes())}`;
  page.drawText(
    safe(
      `Jornada del ${formatInputDate(input.table.date)}  |  Planilla No. ${input.table.journalConsecutive}  |  Generado ${timestamp}`
    ),
    { x: MARGIN, y: y - 12, size: 10, font: regular, color: MUTED }
  );
  y -= 22;

  drawSection('Saldo en caja');
  drawRow('Saldo de movimientos de caja', money(input.table.cashBalance));
  drawRow('Efectivo de la estación del día (paso 2)', money(input.table.dailyCash));
  drawRow('Saldo de efectivo en planilla de ventas', money(input.table.planillaCashBalance), { strong: true });
  drawRow('Saldo acumulado Dominus', money(input.table.dominusAccumulatedBalance));
  drawRow(
    `Diferencia (Dominus - planilla) - ${cashDifferenceLabel(input.differenceStatus)}`,
    input.table.difference === null ? 'Sin dato' : formatSignedCop(input.table.difference),
    { strong: true, shade: true }
  );

  drawSection('Fondo de ahorro para arriendo');
  drawRow('Saldo al día anterior', money(input.table.savingsFund.previousBalance));
  drawRow('Ahorro del día', money(input.table.savingsFund.contribution));
  drawRow('Pagos registrados', money(input.table.savingsFund.payments));
  drawRow('Acumulado a la fecha', money(input.table.savingsFund.accumulatedBalance), {
    strong: true,
    shade: true
  });

  drawSection('Gastos del día (recibos, vales y otros)');
  if (input.table.expenses.items.length === 0) {
    drawRow('No hay gastos administrativos registrados para esta fecha', '');
  } else {
    for (const item of input.table.expenses.items) drawRow(item.description, money(item.amount));
  }
  drawRow('Total de gastos', money(input.table.expenses.total), { strong: true, shade: true });

  const bytes = await document.save();
  const pageCount = document.getPageCount();
  return { bytes, pageCount, balancePages: pageCount - originalPageCount };
}

export function downloadCashBalancePdf(bytes: Uint8Array, fileName: string): void {
  const url = URL.createObjectURL(new Blob([bytes.slice()], { type: 'application/pdf' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10_000);
}
