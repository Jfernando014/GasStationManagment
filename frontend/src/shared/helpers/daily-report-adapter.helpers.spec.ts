import type { Worksheet } from 'exceljs';
import {
  ReportAdaptationError,
  adaptLubricantsFile,
  adaptSalesSheet,
  loadExcelJs,
  readBlob
} from './daily-report-adapter.helpers';
import {
  BACKEND_SALES_HEADERS,
  buildBackendLubricantsSheet,
  buildBackendSalesSheet
} from './daily-report-adapter.test-data';

/**
 * @author javiersolanop777
 */
describe('daily report adapter', () => {
  const PROTOTYPE_SALES_HEADERS = [
    'Vendedor', 'Turno', 'Rol', 'Surtidor', 'Gal. Motor', 'Gal. Diésel', 'Gal. Extra', 'Gal. Max Pro',
    'Canastilla', 'Total tirilla', 'Parciales (sobres)', 'Pico', 'Total efectivo', 'G.A.', 'Credibanco',
    'QR', 'Redenciones', 'Total entregado', 'Descuadre (Dominus − entregado)'
  ];

  async function sheetOf(prmBlob: Blob, prmName: string): Promise<Worksheet> {
    const objExcelJs = await loadExcelJs();
    const objWorkbook = new objExcelJs.Workbook();
    await objWorkbook.xlsx.load(await readBlob(prmBlob));
    return objWorkbook.getWorksheet(prmName) as Worksheet;
  }

  const values = (prmSheet: Worksheet, prmRow: number): unknown[] =>
    PROTOTYPE_SALES_HEADERS.map((_prmHeader, prmIndex) => prmSheet.getRow(prmRow).getCell(prmIndex + 1).value);

  describe('PLANILLA VENTAS', () => {
    it('names the file and the sheet like the prototype', async () => {
      const objFile = await adaptSalesSheet(await buildBackendSalesSheet(), '2026-10-24');
      expect(objFile.fileName).toBe('PLANILLA-VENTAS-2026-10-24.xlsx');
      expect(await sheetOf(objFile.content, 'PLANILLA VENTAS')).toBeTruthy();
    });

    it('writes the title row, an empty row and the header row of the prototype', async () => {
      const objSheet = await sheetOf((await adaptSalesSheet(await buildBackendSalesSheet(), '2026-10-24')).content, 'PLANILLA VENTAS');
      expect(objSheet.getRow(1).getCell(1).value).toBe(
        'PLANILLA VENTAS · EDS Bellavista · Jornada del 24/10/2026 (06:00 a 06:00)'
      );
      expect(values(objSheet, 3)).toEqual(PROTOTYPE_SALES_HEADERS);
    });

    it('moves each value to its column and leaves empty what the backend does not send yet', async () => {
      const objSheet = await sheetOf((await adaptSalesSheet(await buildBackendSalesSheet(), '2026-10-24')).content, 'PLANILLA VENTAS');
      // Vendedor, Turno, Rol(empty), Surtidor, 4 gallons, Canastilla(empty), Total tirilla, Parciales(empty), Pico(empty),
      // Total efectivo, G.A., Credibanco, QR, Redenciones, Total entregado, Descuadre
      expect(values(objSheet, 4)).toEqual([
        'Andres', 'DIA', null, 1, 10, 20, 5, 0, null, 165000, null, null, 100000, 20000, 30000, 10000, 5000, 165000, 0
      ]);
    });

    it('changes the sign of the discrepancy to Dominus - delivered', async () => {
      const objSheet = await sheetOf((await adaptSalesSheet(await buildBackendSalesSheet(), '2026-10-24')).content, 'PLANILLA VENTAS');
      expect(objSheet.getRow(5).getCell(19).value).toBe(5000);
      expect(objSheet.getRow(4).getCell(19).value).toBe(0);
    });

    it('writes the total row as "Total del día"', async () => {
      const objSheet = await sheetOf((await adaptSalesSheet(await buildBackendSalesSheet(), '2026-10-24')).content, 'PLANILLA VENTAS');
      expect(objSheet.getRow(6).getCell(1).value).toBe('Total del día');
      expect(objSheet.getRow(6).getCell(18).value).toBe(245000);
      expect(objSheet.getRow(6).getCell(19).value).toBe(5000);
    });

    it('fails with the names of the missing columns', async () => {
      const arrHeaders = BACKEND_SALES_HEADERS.filter((prmHeader) => prmHeader !== 'Bomba');
      const objPromise = adaptSalesSheet(await buildBackendSalesSheet(arrHeaders), '2026-10-24');
      await expect(objPromise).rejects.toBeInstanceOf(ReportAdaptationError);
      await expect(objPromise).rejects.toThrow('Bomba');
    });

    it('fails when the file is not an Excel file', async () => {
      await expect(adaptSalesSheet(new Blob(['not an excel']), '2026-10-24')).rejects.toBeInstanceOf(
        ReportAdaptationError
      );
    });
  });

  describe('LUBRICANTES Y VARIOS', () => {
    it('names the file like the prototype and keeps only the products', async () => {
      const objFile = await adaptLubricantsFile(await buildBackendLubricantsSheet(), '2026-10-24');
      expect(objFile.fileName).toBe('LUBRICANTES-Y-VARIOS-2026-10-24.xlsx');
      const objSheet = await sheetOf(objFile.content, 'LUBRICANTES Y VARIOS');
      expect(objSheet.getRow(1).getCell(1).value).toBe(
        'LUBRICANTES Y VARIOS · EDS Bellavista · Jornada del 24/10/2026 (06:00 a 06:00)'
      );
      const arrHeader = [1, 2, 3, 4, 5, 6, 7, 8, 9].map((prmColumn) => objSheet.getRow(3).getCell(prmColumn).value);
      expect(arrHeader).toEqual([
        'Código', 'Producto', 'Total unidades', 'Precio', 'Valor',
        'Traslado a mostrador', 'Queda en oficina', 'Queda en mostrador', 'Existencia'
      ]);
    });

    it('moves units, price and value, and leaves the inventory columns empty', async () => {
      const objSheet = await sheetOf(
        (await adaptLubricantsFile(await buildBackendLubricantsSheet(), '2026-10-24')).content,
        'LUBRICANTES Y VARIOS'
      );
      const arrRow = [1, 2, 3, 4, 5, 6, 7, 8, 9].map((prmColumn) => objSheet.getRow(4).getCell(prmColumn).value);
      expect(arrRow).toEqual(['80216', 'Aceite Test', 5, 20000, 100000, null, null, null, null]);
      expect(objSheet.getRow(6).getCell(2).value).toBe('Aditivo Test');
    });

    it('does not write the totals or the basket summary, and adds the note of the prototype', async () => {
      const objSheet = await sheetOf(
        (await adaptLubricantsFile(await buildBackendLubricantsSheet(), '2026-10-24')).content,
        'LUBRICANTES Y VARIOS'
      );
      expect(objSheet.getRow(7).getCell(2).value).toBeNull();
      expect(objSheet.getRow(8).getCell(1).value).toBe('Precios provisionales: pendientes de confirmar con el cliente.');
      expect(objSheet.rowCount).toBe(8);
    });
  });
});
