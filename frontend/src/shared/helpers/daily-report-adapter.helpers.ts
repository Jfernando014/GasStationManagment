import type { CellValue, Worksheet } from 'exceljs';
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

/** A column of the prototype and the column of the backend that feeds it (null = not sent yet). */
interface ColumnSpec {
  header: string;
  source: string | null;
  /** True when the prototype uses the opposite sign of the backend. */
  negate?: boolean;
}

export const XLSX_MIME = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

const STATION_NAME = 'Bellavista';
const SALES_TITLE = 'PLANILLA VENTAS';
const LUBRICANTS_TITLE = 'LUBRICANTES Y VARIOS';
const BACKEND_SALES_TOTAL = 'TOTAL DEL DÍA';
const BACKEND_LUBRICANTS_TOTAL = 'TOTALES';
const PROTOTYPE_SALES_TOTAL = 'Total del día';
const PROTOTYPE_NOTE = 'Precios provisionales: pendientes de confirmar con el cliente.';

/**
 * PLANILLA VENTAS. The discrepancy of the prototype is Dominus - delivered, and the backend sends
 * delivered - Dominus, so the sign changes.
 */
const SALES_COLUMNS: readonly ColumnSpec[] = [
  { header: 'Vendedor', source: 'Vendedor' },
  { header: 'Turno', source: 'Turno' },
  { header: 'Rol', source: null },
  { header: 'Surtidor', source: 'Bomba' },
  { header: 'Gal. Motor', source: 'Galones Motor' },
  { header: 'Gal. Diésel', source: 'Galones Diesel' },
  { header: 'Gal. Extra', source: 'Galones Extra' },
  { header: 'Gal. Max Pro', source: 'Galones Maxpro' },
  { header: 'Canastilla', source: null },
  { header: 'Total tirilla', source: 'Tirilla Dominus' },
  { header: 'Parciales (sobres)', source: null },
  { header: 'Pico', source: null },
  { header: 'Total efectivo', source: 'Efectivo' },
  { header: 'G.A.', source: 'GA' },
  { header: 'Credibanco', source: 'Credibanco' },
  { header: 'QR', source: 'QR' },
  { header: 'Redenciones', source: 'Redenciones' },
  { header: 'Total entregado', source: 'Total entregado' },
  { header: 'Descuadre (Dominus − entregado)', source: 'Diferencia', negate: true }
];

/**
 * LUBRICANTES Y VARIOS. The prototype also has one column of units for each vendor, between
 * "Producto" and "Total unidades". They are not here because the backend does not send the units
 * by vendor yet.
 */
const LUBRICANTS_COLUMNS: readonly ColumnSpec[] = [
  { header: 'Código', source: 'Código' },
  { header: 'Producto', source: 'Producto' },
  { header: 'Total unidades', source: 'Unidades' },
  { header: 'Precio', source: 'Precio unidad' },
  { header: 'Valor', source: 'Valor venta' },
  { header: 'Traslado a mostrador', source: null },
  { header: 'Queda en oficina', source: null },
  { header: 'Queda en mostrador', source: null },
  { header: 'Existencia', source: null }
];

/** Loads ExcelJS only when it is needed, so it is not part of the main bundle. */
export async function loadExcelJs(): Promise<typeof import('exceljs')> {
  const objModule = await import('exceljs');
  return (objModule as unknown as { default?: typeof import('exceljs') }).default ?? objModule;
}

/** PLANILLA VENTAS of the backend -> PLANILLA VENTAS of the prototype. */
export async function adaptSalesSheet(prmContent: Blob, prmDate: string): Promise<DownloadedFile> {
  const objSheet = await openSheet(prmContent, SALES_TITLE);
  const objRecords = readRecords(objSheet, SALES_TITLE, 'Vendedor', requiredSources(SALES_COLUMNS));
  const objRows = objRecords
    .filter((prmRecord) => prmRecord['Vendedor'] !== BACKEND_SALES_TOTAL)
    .map((prmRecord) => toRow(SALES_COLUMNS, prmRecord));
  const objTotal = objRecords.find((prmRecord) => prmRecord['Vendedor'] === BACKEND_SALES_TOTAL);
  if (objTotal) {
    const objTotalRow = toRow(SALES_COLUMNS, objTotal);
    objTotalRow[0] = PROTOTYPE_SALES_TOTAL;
    objRows.push(objTotalRow);
  }
  return buildFile(SALES_TITLE, prmDate, SALES_COLUMNS, objRows, []);
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
  prmSheet.getRow(varHeaderRow).eachCell((prmCell, prmColumn) => {
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
  prmFooter: CellOut[][]
): Promise<DownloadedFile> {
  const [varYear, varMonth, varDay] = prmDate.split('-');
  const objExcelJs = await loadExcelJs();
  const objWorkbook = new objExcelJs.Workbook();
  const objSheet = objWorkbook.addWorksheet(prmTitle.slice(0, 31));
  objSheet.addRow([`${prmTitle} · EDS ${STATION_NAME} · Jornada del ${varDay}/${varMonth}/${varYear} (06:00 a 06:00)`]);
  objSheet.addRow([]);
  objSheet.addRow(prmColumns.map((prmColumn) => prmColumn.header));
  [...prmRows, ...prmFooter].forEach((prmRow) => objSheet.addRow(prmRow));
  prmColumns.forEach((_prmColumn, prmIndex) => {
    objSheet.getColumn(prmIndex + 1).width = prmIndex === 0 ? 30 : 16;
  });
  const objBuffer = await objWorkbook.xlsx.writeBuffer();
  return {
    fileName: `${prmTitle.replace(/\s+/g, '-')}-${prmDate}.xlsx`,
    content: new Blob([objBuffer as BlobPart], { type: XLSX_MIME })
  };
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
    return prmValue.richText.map((prmPart) => prmPart.text).join('');
  }
  return String(prmValue);
}

function text(prmValue: CellValue): string {
  return String(plain(prmValue) ?? '').trim();
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
