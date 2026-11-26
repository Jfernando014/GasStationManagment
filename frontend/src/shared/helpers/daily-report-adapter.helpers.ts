import type { Cell, CellValue, RichText, Worksheet } from 'exceljs';
import { DownloadedFile } from '../models/daily-download.model';

/**
 * Adapter of the daily reports. The backend builds the Excel files with its own layout, and the
 * client expects the layout of the prototype. This file reads the Excel of the backend by column
 * name (not by position) and writes a new Excel with the layout of the prototype.
 *
 * Columns that the backend does not send yet (Rol, Canastilla, Parciales, Pico, the units of each
 * vendor, and the inventory columns of the lubricants) are written empty. When the backend sends
 * them, only the "source" of that column has to be filled in the column lists below.
 *
 * @author javiersolanop777
 */

/** Thrown when the Excel of the backend is not valid or does not have the expected layout. */
export class ReportAdaptationError extends Error {
  constructor(prmMessage: string) {
    super(prmMessage);
    this.name = 'ReportAdaptationError';
  }
}

type CellOut = string | number | null;
type RowRecord = Record<string, CellOut>;

/** Number format of a column, applied when the file is written. */
type ColumnFormat = 'money' | 'gallons' | 'integer';

/** A column of the prototype and the column of the backend that feeds it (null = not sent yet). */
interface ColumnSpec {
  header: string;
  source: string | null;
  /** True when the prototype uses the opposite sign of the backend. */
  negate?: boolean;
  /** Width in characters, for a column whose text does not fit in the default one. */
  width?: number;
  /** Number format of the column. Omitted for text columns. */
  format?: ColumnFormat;
  /** True for the columns painted with the vendor result (CUADRA/SOBRA/FALTA). */
  alert?: boolean;
}

export const XLSX_MIME = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

const STATION_NAME = 'Bellavista';
const SALES_TITLE = 'PLANILLA VENTAS';
const LUBRICANTS_TITLE = 'LUBRICANTES Y VARIOS';
const BACKEND_SALES_TOTAL = 'TOTAL DEL DÍA';
const BACKEND_LUBRICANTS_TOTAL = 'TOTALES';
const PROTOTYPE_SALES_TOTAL = 'Total del día';
const PROTOTYPE_NOTE = 'Precios provisionales: pendientes de confirmar con el cliente.';

/** ExcelJS has no auto-fit: the width follows the longest text of the column, within these limits. */
const MIN_COLUMN_WIDTH = 10;
const MAX_COLUMN_WIDTH = 42;

/**
 * PLANILLA VENTAS. The discrepancy of the prototype is Dominus - delivered, and the backend sends
 * delivered - Dominus, so the sign changes.
 */
const SALES_COLUMNS: readonly ColumnSpec[] = [
  { header: 'Vendedor', source: 'Vendedor' },
  // The longest shift is a support one written as two blocks of hours: "06:00–09:00 y 17:00–24:00"
  { header: 'Turno', source: 'Turno', width: 27 },
  { header: 'Rol', source: null },
  { header: 'Surtidor', source: 'Bomba', format: 'integer' },
  { header: 'Gal. Motor', source: 'Galones Motor', format: 'gallons' },
  { header: 'Gal. Diésel', source: 'Galones Diesel', format: 'gallons' },
  { header: 'Gal. Extra', source: 'Galones Extra', format: 'gallons' },
  { header: 'Gal. Max Pro', source: 'Galones Maxpro', format: 'gallons' },
  { header: 'Canastilla', source: null, format: 'money' },
  { header: 'Total tirilla', source: 'Tirilla Dominus', format: 'money' },
  { header: 'Parciales (sobres)', source: null, format: 'money' },
  { header: 'Pico', source: null, format: 'money' },
  { header: 'Total efectivo', source: 'Efectivo', format: 'money' },
  { header: 'G.A.', source: 'GA', format: 'money' },
  { header: 'Credibanco', source: 'Credibanco', format: 'money' },
  { header: 'QR', source: 'QR', format: 'money' },
  { header: 'Redenciones', source: 'Redenciones', format: 'money' },
  { header: 'Total entregado', source: 'Total entregado', format: 'money' },
  { header: 'Descuadre (Dominus − entregado)', source: 'Diferencia', negate: true, format: 'money', alert: true }
];

/**
 * LUBRICANTES Y VARIOS. The prototype also has one column of units for each vendor, between
 * "Producto" and "Total unidades". They are not here because the backend does not send the units
 * by vendor yet.
 */
const LUBRICANTS_COLUMNS: readonly ColumnSpec[] = [
  { header: 'Código', source: 'Código' },
  { header: 'Producto', source: 'Producto' },
  { header: 'Total unidades', source: 'Unidades', format: 'integer' },
  { header: 'Precio', source: 'Precio unidad', format: 'money' },
  { header: 'Valor', source: 'Valor venta', format: 'money' },
  { header: 'Traslado a mostrador', source: null, format: 'integer' },
  { header: 'Queda en oficina', source: null, format: 'integer' },
  { header: 'Queda en mostrador', source: null, format: 'integer' },
  { header: 'Existencia', source: null, format: 'integer' }
];

/** Loads ExcelJS only when it is needed, so it is not part of the main bundle. */
export async function loadExcelJs(): Promise<typeof import('exceljs')> {
  const objModule = await import('exceljs');
  return (objModule as unknown as { default?: typeof import('exceljs') }).default ?? objModule;
}

/** PLANILLA VENTAS of the backend -> PLANILLA VENTAS of the prototype. */
export async function adaptSalesSheet(prmContent: Blob, prmDate: string): Promise<DownloadedFile> {
  const objSheet = await openSheet(prmContent, SALES_TITLE);
  const objRecords = readRecords(objSheet, SALES_TITLE, 'Vendedor',
    [...requiredSources(SALES_COLUMNS), 'Resultado']);
  const objVendors = objRecords.filter((prmRecord) => prmRecord['Vendedor'] !== BACKEND_SALES_TOTAL);
  const objRows = objVendors.map((prmRecord) => toRow(SALES_COLUMNS, prmRecord));
  // Colours only apply to vendor rows; the total row gets its own styling (green) and is skipped
  // by the varTotalRow guard in styleSheet.  Index the statuses only for vendors.
  const objStatuses = objVendors.map((prmRecord) => resultStatusOf(prmRecord['Resultado']));
  const objTotal = objRecords.find((prmRecord) => prmRecord['Vendedor'] === BACKEND_SALES_TOTAL);
  if (objTotal) {
    const objTotalRow = toRow(SALES_COLUMNS, objTotal);
    objTotalRow[0] = PROTOTYPE_SALES_TOTAL;
    objRows.push(objTotalRow);
    objStatuses.push(null);
  }
  return buildFile(SALES_TITLE, prmDate, SALES_COLUMNS, objRows, [], objStatuses);
}

/** LUBRICANTES Y VARIOS of the backend -> LUBRICANTES Y VARIOS of the prototype. */
export async function adaptLubricantsFile(prmContent: Blob, prmDate: string): Promise<DownloadedFile> {
  const objSheet = await openSheet(prmContent, LUBRICANTS_TITLE);
  const objRecords = readRecords(objSheet, LUBRICANTS_TITLE, 'Código', requiredSources(LUBRICANTS_COLUMNS));
  // The prototype has no totals or basket summary: only the products, before the TOTALES row
  const varEnd = objRecords.findIndex((prmRecord) => prmRecord['Producto'] === BACKEND_LUBRICANTS_TOTAL);
  const objProducts = varEnd >= 0 ? objRecords.slice(0, varEnd) : objRecords;
  const objRows = objProducts.map((prmRecord) => toRow(LUBRICANTS_COLUMNS, prmRecord));
  return buildFile(LUBRICANTS_TITLE, prmDate, LUBRICANTS_COLUMNS, objRows, [[], [PROTOTYPE_NOTE]]);
}

function requiredSources(prmColumns: readonly ColumnSpec[]): string[] {
  return prmColumns.map((prmColumn) => prmColumn.source).filter((prmSource): prmSource is string => prmSource !== null);
}

/** Result column of the backend: CUADRA, SOBRA or FALTA. */
function resultStatusOf(prmValue: CellOut): string | null {
  return typeof prmValue === 'string' && prmValue.trim() !== '' ? prmValue.trim() : null;
}

async function openSheet(prmContent: Blob, prmSheetName: string): Promise<Worksheet> {
  const objExcelJs = await loadExcelJs();
  const objWorkbook = new objExcelJs.Workbook();
  try {
    await objWorkbook.xlsx.load(await readBlob(prmContent));
  } catch {
    throw new ReportAdaptationError('The file of the server is not a valid Excel file');
  }
  const objSheet = objWorkbook.getWorksheet(prmSheetName);
  if (!objSheet) {
    throw new ReportAdaptationError(`The sheet "${prmSheetName}" was not found`);
  }
  return objSheet;
}

/** Reads the rows under the header row. The header row is found by the text of its first cell. */
function readRecords(prmSheet: Worksheet, prmSheetName: string, prmFirstHeader: string, prmRequired: string[]): RowRecord[] {
  let varHeaderRow = -1;
  for (let varIndex = 1; varIndex <= Math.min(prmSheet.rowCount, 10); varIndex++) {
    if (text(prmSheet.getRow(varIndex).getCell(1).value) === prmFirstHeader) {
      varHeaderRow = varIndex;
      break;
    }
  }
  if (varHeaderRow < 0) {
    throw new ReportAdaptationError(`The header row of "${prmSheetName}" was not found`);
  }
  const objColumns = new Map<string, number>();
  prmSheet.getRow(varHeaderRow).eachCell((prmCell: Cell, prmColumn: number) => {
    objColumns.set(text(prmCell.value), prmColumn);
  });
  const arrMissing = prmRequired.filter((prmName) => !objColumns.has(prmName));
  if (arrMissing.length > 0) {
    throw new ReportAdaptationError(`Missing columns in "${prmSheetName}": ${arrMissing.join(', ')}`);
  }
  const objRecords: RowRecord[] = [];
  for (let varIndex = varHeaderRow + 1; varIndex <= prmSheet.rowCount; varIndex++) {
    const objRow = prmSheet.getRow(varIndex);
    const objRecord: RowRecord = {};
    let varEmpty = true;
    objColumns.forEach((prmColumn, prmName) => {
      const objValue = plain(objRow.getCell(prmColumn).value);
      if (objValue !== null && objValue !== '') {
        varEmpty = false;
      }
      objRecord[prmName] = objValue;
    });
    if (!varEmpty) {
      objRecords.push(objRecord);
    }
  }
  return objRecords;
}

function toRow(prmColumns: readonly ColumnSpec[], prmRecord: RowRecord): CellOut[] {
  return prmColumns.map((prmColumn) => {
    if (prmColumn.source === null) {
      return null;
    }
    const objValue = prmRecord[prmColumn.source] ?? null;
    return prmColumn.negate && typeof objValue === 'number' ? negate(objValue) : objValue;
  });
}

/** Changes the sign. Zero stays zero (no "-0"). */
function negate(prmValue: number): number {
  return prmValue === 0 ? 0 : -prmValue;
}

/** Same file layout as the prototype: title row, empty row, header row, data. */
async function buildFile(
  prmTitle: string,
  prmDate: string,
  prmColumns: readonly ColumnSpec[],
  prmRows: CellOut[][],
  prmFooter: CellOut[][],
  prmStatuses: readonly (string | null)[] = []
): Promise<DownloadedFile> {
  const [varYear, varMonth, varDay] = prmDate.split('-');
  const objExcelJs = await loadExcelJs();
  const objWorkbook = new objExcelJs.Workbook();
  const objSheet = objWorkbook.addWorksheet(prmTitle.slice(0, 31));
  objSheet.addRow([`${prmTitle} · EDS ${STATION_NAME} · Jornada del ${varDay}/${varMonth}/${varYear} (06:00 a 06:00)`]);
  objSheet.addRow([]);
  objSheet.addRow(prmColumns.map((prmColumn) => prmColumn.header));
  [...prmRows, ...prmFooter].forEach((prmRow) => objSheet.addRow(prmRow));
  prmColumns.forEach((prmColumn, prmIndex) => {
    const varLongest = Math.max(
      prmColumn.header.length,
      ...prmRows.map((prmRow) => textWidth(prmRow[prmIndex], prmColumn.format))
    );
    objSheet.getColumn(prmIndex + 1).width = prmColumn.width ?? Math.min(
      Math.max(varLongest + 2, MIN_COLUMN_WIDTH),
      MAX_COLUMN_WIDTH
    );
  });
  styleSheet(objSheet, prmColumns, prmRows.length, prmStatuses);
  const objBuffer = await objWorkbook.xlsx.writeBuffer();
  return {
    fileName: `${prmTitle.replace(/\s+/g, '-')}-${prmDate}.xlsx`,
    content: new Blob([objBuffer as BlobPart], { type: XLSX_MIME })
  };
}

/**
 * Visual identity of the client's template (BELLAVISTA.xlsx): navy header with white text,
 * light green for the total row, thin borders on every cell and the number formats of each column.
 * It is the same palette the backend uses, so both files of the day look alike.
 */
const PALETTE = {
  navy: 'FF1B365D',
  white: 'FFFFFFFF',
  border: 'FFD9D9D9',
  greenFill: 'FFE2EFDA',
  greenText: 'FF1E4620',
  amberFill: 'FFFFF2CC',
  amberText: 'FF7F6000',
  redFill: 'FFFFC7CE',
  redText: 'FF9C0006',
  greyText: 'FF595959'
} as const;

const NUMBER_FORMATS: Record<ColumnFormat, string> = {
  money: '"$" #,##0',
  gallons: '#,##0.00',
  integer: '#,##0'
};

/** Characters the number format adds: currency symbol, decimals and thousand separators. */
const FORMAT_PADDING: Record<ColumnFormat, number> = { money: 4, gallons: 3, integer: 2 };

/** Colours of the vendor result: balanced green, surplus amber and shortfall red. */
const RESULT_COLOURS: Record<string, { fill: string; text: string }> = {
  CUADRA: { fill: PALETTE.greenFill, text: PALETTE.greenText },
  OK: { fill: PALETTE.greenFill, text: PALETTE.greenText },
  SOBRA: { fill: PALETTE.amberFill, text: PALETTE.amberText },
  FALTA: { fill: PALETTE.redFill, text: PALETTE.redText }
};

const THIN_BORDER = {
  top: { style: 'thin' as const, color: { argb: PALETTE.border } },
  bottom: { style: 'thin' as const, color: { argb: PALETTE.border } },
  left: { style: 'thin' as const, color: { argb: PALETTE.border } },
  right: { style: 'thin' as const, color: { argb: PALETTE.border } }
};

/** Applies the palette and the formats to the title, the header, the data and the note rows. */
function styleSheet(
  prmSheet: Worksheet,
  prmColumns: readonly ColumnSpec[],
  prmDataRows: number,
  prmStatuses: readonly (string | null)[]
): void {
  prmSheet.views = [{ state: 'frozen', ySplit: 3 }];

  const objTitle = prmSheet.getRow(1);
  objTitle.height = 24;
  objTitle.getCell(1).font = { bold: true, size: 14, color: { argb: PALETTE.navy } };

  const objHeader = prmSheet.getRow(3);
  objHeader.height = 30;
  prmColumns.forEach((_prmColumn, prmIndex) => {
    const objCell = objHeader.getCell(prmIndex + 1);
    objCell.font = { bold: true, size: 11, color: { argb: PALETTE.white } };
    objCell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: PALETTE.navy } };
    objCell.alignment = { horizontal: 'center', vertical: 'middle', wrapText: true };
    objCell.border = THIN_BORDER;
  });

  const varTotalRow = isTotalRow(prmSheet, 3 + prmDataRows) ? 3 + prmDataRows : 0;
  for (let varRow = 4; varRow <= 3 + prmDataRows; varRow++) {
    const objRow = prmSheet.getRow(varRow);
    prmColumns.forEach((objColumn, prmIndex) => {
      const objCell = objRow.getCell(prmIndex + 1);
      objCell.border = THIN_BORDER;
      if (objColumn.format) {
        objCell.numFmt = NUMBER_FORMATS[objColumn.format];
      }
      if (objColumn.format && typeof objCell.value === 'number') {
        objCell.alignment = { horizontal: 'right' };
      }
      if (varRow === varTotalRow) {
        objCell.font = { bold: true, color: { argb: PALETTE.greenText } };
        objCell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: PALETTE.greenFill } };
        objCell.border = { ...THIN_BORDER, top: { style: 'double', color: { argb: PALETTE.greenText } } };
      } else if (objColumn.alert && typeof objCell.value === 'number') {
        // Only vendor rows carry a result, and the result decides the colour: a tolerance is green.
        const objColours = resultColours(prmStatuses[varRow - 4] ?? null);
        if (objColours) {
          objCell.font = { bold: true, color: { argb: objColours.text } };
          objCell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: objColours.fill } };
        }
      }
    });
  }

  const objLastRow = prmSheet.getRow(prmSheet.rowCount);
  if (objLastRow.getCell(1).value === PROTOTYPE_NOTE) {
    objLastRow.getCell(1).font = { italic: true, size: 10, color: { argb: PALETTE.greyText } };
  }
}

/** True when the given row starts with the total label of the prototype. */
function isTotalRow(prmSheet: Worksheet, prmRow: number): boolean {
  return prmRow > 3 && prmSheet.getRow(prmRow).getCell(1).value === PROTOTYPE_SALES_TOTAL;
}

/** Colours of a vendor result, or null when the row has no result (for example the total row). */
function resultColours(prmStatus: string | null): { fill: string; text: string } | null {
  return prmStatus ? RESULT_COLOURS[prmStatus.trim().toUpperCase()] ?? null : null;
}

function plain(prmValue: CellValue): CellOut {
  if (prmValue === null || prmValue === undefined) {
    return null;
  }
  if (typeof prmValue === 'number' || typeof prmValue === 'string') {
    return prmValue;
  }
  if (typeof prmValue === 'boolean') {
    return prmValue ? 1 : 0;
  }
  if (prmValue instanceof Date) {
    return prmValue.toISOString();
  }
  if ('result' in prmValue) {
    return plain(prmValue.result as CellValue);
  }
  if ('richText' in prmValue) {
    return prmValue.richText.map((prmPart: RichText) => prmPart.text).join('');
  }
  return String(prmValue);
}

function text(prmValue: CellValue): string {
  return String(plain(prmValue) ?? '').trim();
}

/** Approximate width in characters of a cell value, used to fit the columns without ExcelJS auto-fit. */
function textWidth(prmValue: CellOut, prmFormat?: ColumnFormat): number {
  if (prmValue === null) {
    return 0;
  }
  const varLength = String(prmValue).length;
  return typeof prmValue === 'number' && prmFormat ? varLength + FORMAT_PADDING[prmFormat] : varLength;
}

/** Blob.arrayBuffer is not in every browser or test environment, so FileReader is the fallback. */
export function readBlob(prmBlob: Blob): Promise<ArrayBuffer> {
  if (typeof prmBlob.arrayBuffer === 'function') {
    return prmBlob.arrayBuffer();
  }
  return new Promise((prmResolve, prmReject) => {
    const objReader = new FileReader();
    objReader.onload = () => prmResolve(objReader.result as ArrayBuffer);
    objReader.onerror = () => prmReject(objReader.error);
    objReader.readAsArrayBuffer(prmBlob);
  });
}
