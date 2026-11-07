import { loadExcelJs } from './daily-report-adapter.helpers';

/**
 * Builds Excel files with the SAME layout as the ones of the backend (DailyDownloadWorkbookExporter),
 * for the tests of the adapter. Data: Andres (CUADRA) and Milena (FALTA 5000).
 *
 * @author javiersolanop777
 */

export const BACKEND_SALES_HEADERS = [
  'Vendedor', 'Documento', 'Turno', 'Bomba', 'Galones Motor', 'Galones Diesel', 'Galones Extra',
  'Galones Maxpro', 'Total galones', 'Unidades aditivos', 'Efectivo', 'GA', 'Credibanco', 'QR',
  'Redenciones', 'Total entregado', 'Tirilla Dominus', 'Diferencia', 'Resultado'
];

export const BACKEND_LUBRICANTS_HEADERS = ['Código', 'Producto', 'Grupo', 'Precio unidad', 'Unidades', 'Valor venta'];

type Cell = string | number | null;

async function toBlob(prmSheetName: string, prmRows: Cell[][]): Promise<Blob> {
  const objExcelJs = await loadExcelJs();
  const objWorkbook = new objExcelJs.Workbook();
  const objSheet = objWorkbook.addWorksheet(prmSheetName);
  prmRows.forEach((prmRow) => objSheet.addRow(prmRow));
  const objBuffer = await objWorkbook.xlsx.writeBuffer();
  return new Blob([objBuffer as BlobPart]);
}

/** Layout of the backend: title (row 1), date (row 2), empty (row 3), headers (row 4), data, total. */
export function buildBackendSalesSheet(prmHeaders: string[] = BACKEND_SALES_HEADERS): Promise<Blob> {
  const objAll = BACKEND_SALES_HEADERS;
  const pick = (prmRow: Cell[]): Cell[] => prmHeaders.map((prmHeader) => prmRow[objAll.indexOf(prmHeader)]);
  return toBlob('PLANILLA VENTAS', [
    ['PLANILLA VENTAS'],
    ['Fecha: 2026-10-24'],
    [],
    prmHeaders,
    pick(['Andres', '1061', 'DIA', 1, 10, 20, 5, 0, 35, 2, 100000, 20000, 30000, 10000, 5000, 165000, 165000, 0, 'CUADRA']),
    pick(['Milena', '1062', 'NOCHE', 2, 5, 5, 0, 2, 12, 0, 80000, 0, 0, 0, 0, 80000, 85000, -5000, 'FALTA']),
    pick(['TOTAL DEL DÍA', null, null, null, 15, 25, 5, 2, 47, 2, 180000, 20000, 30000, 10000, 5000, 245000, 250000, -5000, null])
  ]);
}

/** Layout of the backend: products, TOTALES row, empty row and the basket summary. */
export function buildBackendLubricantsSheet(): Promise<Blob> {
  return toBlob('LUBRICANTES Y VARIOS', [
    ['LUBRICANTES Y VARIOS'],
    ['Fecha: 2026-10-24'],
    [],
    BACKEND_LUBRICANTS_HEADERS,
    ['80216', 'Aceite Test', 'ACEITES', 20000, 5, 100000],
    ['76001', 'Bolsas Test', 'MERCANCIA VARIA', 1000, 5, 5000],
    ['76102', 'Aditivo Test', 'ADITIVOS Y OTROS', 10000, 2, 20000],
    [null, 'TOTALES', null, null, 12, 125000],
    [],
    [null, 'VENTA ACEITES', null, null, null, 100000],
    [null, 'VENTA MERCANCIA VARIA', null, null, null, 5000],
    [null, 'VENTA ADITIVOS Y OTROS', null, null, null, 20000],
    [null, 'VENTA TOTAL CANASTA', null, null, null, 125000]
  ]);
}
