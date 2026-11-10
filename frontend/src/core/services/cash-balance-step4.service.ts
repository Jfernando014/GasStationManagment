import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import {
  BackendCashBalanceStepResponseDto,
  ConsolidadoCuadreState,
  DailyExpenseItem,
  SavingsFundPaymentItem,
  SavingsFundState,
  Step4CalculationResult,
  Step4CapturedData
} from '../../shared/models/dtos/cash-balance-step4.dto';
import { VendorCloseSummaryDto } from '../../shared/models/dtos/vendor-close.dto';

interface SeedSaldoCajaRow {
  date: string;
  planilla: number;
  dineroAnterior: number;
  consignaciones: number;
  ahorroAcumulado: number;
  recibosGA: number;
  saldoDominus: number;
}

@Injectable({
  providedIn: 'root'
})
export class CashBalanceStep4Service {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/v1/sales';

  private static readonly STORAGE_CAPTURED_KEY = 'eds_step4_captured_v1';
  private static readonly STORAGE_PAYMENTS_KEY = 'eds_step4_fund_payments_v1';

  private readonly fallbackStorage = new Map<string, string>();

  private readStorage(key: string): string | null {
    try {
      if (typeof localStorage !== 'undefined' && typeof localStorage.getItem === 'function') {
        return localStorage.getItem(key);
      }
    } catch {
      // fall through
    }
    return this.fallbackStorage.get(key) ?? null;
  }

  private writeStorage(key: string, value: string): void {
    this.fallbackStorage.set(key, value);
    try {
      if (typeof localStorage !== 'undefined' && typeof localStorage.setItem === 'function') {
        localStorage.setItem(key, value);
      }
    } catch {
      // ignore storage errors
    }
  }

  clearAllStoredData(): void {
    this.fallbackStorage.clear();
    try {
      if (typeof localStorage !== 'undefined' && typeof localStorage.removeItem === 'function') {
        localStorage.removeItem(CashBalanceStep4Service.STORAGE_CAPTURED_KEY);
        localStorage.removeItem(CashBalanceStep4Service.STORAGE_PAYMENTS_KEY);
      }
    } catch {
      // ignore storage errors
    }
  }

  readonly PLANILLA_BASE = { fecha: '2026-08-01', numero: 289 };
  readonly TOLERANCIA_CONSOLIDADO = 1;
  readonly TOLERANCIA_SALDO = 0;
  readonly FONDO_INICIO = '2026-07-21';
  readonly AHORRO_DIARIO_VIGENTE = 250000;

  /**
   * Planillas históricas del Excel "4 SALDO EN CAJA BELLAVISTA AGOSTO 2026", Hoja 1
   */
  private readonly saldoCajaSeed: SeedSaldoCajaRow[] = [
    {
      date: '2026-08-01',
      planilla: 289,
      dineroAnterior: 9364000,
      consignaciones: 0,
      ahorroAcumulado: 3000000,
      recibosGA: 0,
      saldoDominus: 24629469
    }
  ];

  /**
   * Gastos administrativos auténticos CC 761 (9 GASTOS ADMINISTRATIVOS BELLAVISTA AGOSTO 2026.xlsx)
   */
  private readonly adminExpensesSeed: DailyExpenseItem[] = [
    { id: 1, date: '2026-08-01', razonSocial: 'Ofimatic@.NET', concepto: '20 IMPRESIONES B/N', valor: 6000 },
    { id: 2, date: '2026-08-01', razonSocial: 'Servientrega S.A', concepto: 'Envío documentos Bogotá', valor: 18350 },
    { id: 3, date: '2026-08-03', razonSocial: 'Mónica Sirley Dorado', concepto: 'Apoyo crispetas', valor: 20000 },
    { id: 4, date: '2026-08-04', razonSocial: 'Jeronimo Martins Colombia S.A.S.', concepto: 'Insumos crispetas', valor: 21750 },
    { id: 5, date: '2026-08-04', razonSocial: 'Ofimatic@.NET', concepto: '20 IMPRESIONES B/N', valor: 3500 },
    { id: 6, date: '2026-08-06', razonSocial: 'Super sorpresa Panadería - Pastelería', concepto: 'Torta cumpleaños', valor: 46500 },
    { id: 7, date: '2026-08-14', razonSocial: 'Casa Yale', concepto: 'Arreglo chapa oficina', valor: 20000 },
    { id: 8, date: '2026-08-18', razonSocial: 'Ofimatic@.NET', concepto: '8 IMP B/N', valor: 2400 },
    { id: 9, date: '2026-08-18', razonSocial: 'Servientrega S.A', concepto: 'Envío documentos Bogotá', valor: 18350 },
    { id: 10, date: '2026-08-20', razonSocial: 'Ofimatic@.NET', concepto: 'Impresiones, sobres y carpetas', valor: 10500 },
    { id: 11, date: '2026-08-27', razonSocial: 'Ofimatic@.NET', concepto: '2 impresiones', valor: 600 },
    { id: 12, date: '2026-08-27', razonSocial: 'Eduar Quila', concepto: 'Transporte canecas', valor: 20000 }
  ];

  /**
   * Cierres históricos por fecha para cálculo de efectivo y tirilla cuando no se ha guardado paso 2 en vivo
   */
  private readonly historicalDayTotals: Record<string, { efectivo: number; tirilla: number }> = {
    '2026-08-01': { efectivo: 7676000 + 4840000, tirilla: 11783503 + 6522327 },
    '2026-08-05': { efectivo: 2373000 + 1847000 + 3310000, tirilla: 3890028.87 + 4285510.58 + 5961277.95 },
    '2026-08-07': { efectivo: 4730000 + 2572000, tirilla: 7264388 + 3389365 },
    '2026-08-17': { efectivo: 3629000 + 2550000, tirilla: 4605131 + 3747544 }
  };

  private readonly consignacionesSeed: { date: string; amount: number }[] = [
    { date: '2026-08-01', amount: 10450000 }
  ];

  private pdfMemoryStore: Record<string, { name: string; bytes: Uint8Array; file: File; finalUrl?: string }> = {};

  diasEntreFechas(desde: string, hasta: string): number {
    const parseUtc = (iso: string): number => {
      const [y, m, d] = iso.split('-').map(Number);
      return Date.UTC(y, m - 1, d);
    };
    return Math.round((parseUtc(hasta) - parseUtc(desde)) / 86400000);
  }

  planillaForDate(fecha: string, journalConsecutive?: number | null): number {
    if (typeof journalConsecutive === 'number' && journalConsecutive > 0 && journalConsecutive >= 289) {
      return journalConsecutive;
    }
    return this.PLANILLA_BASE.numero + this.diasEntreFechas(this.PLANILLA_BASE.fecha, fecha);
  }

  previousDateIso(fecha: string): string {
    const [y, m, d] = fecha.split('-').map(Number);
    const prev = new Date(y, m - 1, d - 1);
    const py = prev.getFullYear();
    const pm = String(prev.getMonth() + 1).padStart(2, '0');
    const pd = String(prev.getDate()).padStart(2, '0');
    return `${py}-${pm}-${pd}`;
  }

  getCapturedData(fecha: string): Step4CapturedData {
    try {
      const raw = this.readStorage(CashBalanceStep4Service.STORAGE_CAPTURED_KEY);
      if (!raw) return {};
      const all = JSON.parse(raw) as Record<string, Step4CapturedData>;
      return all[fecha] ?? {};
    } catch {
      return {};
    }
  }

  saveCapturedData(fecha: string, partial: Partial<Step4CapturedData>): Step4CapturedData {
    try {
      const raw = this.readStorage(CashBalanceStep4Service.STORAGE_CAPTURED_KEY);
      const all: Record<string, Step4CapturedData> = raw ? JSON.parse(raw) : {};
      const updated: Step4CapturedData = { ...(all[fecha] ?? {}), ...partial };
      all[fecha] = updated;
      this.writeStorage(CashBalanceStep4Service.STORAGE_CAPTURED_KEY, JSON.stringify(all));
      return updated;
    } catch {
      return { ...partial };
    }
  }

  getStoredFundPayments(): SavingsFundPaymentItem[] {
    try {
      const raw = this.readStorage(CashBalanceStep4Service.STORAGE_PAYMENTS_KEY);
      return raw ? (JSON.parse(raw) as SavingsFundPaymentItem[]) : [];
    } catch {
      return [];
    }
  }

  registerFundPayment(fecha: string, valor: number, concepto = 'Arriendo'): Observable<SavingsFundPaymentItem> {
    const payment: SavingsFundPaymentItem = {
      id: Date.now().toString(36),
      fecha,
      valor,
      concepto
    };
    const current = this.getStoredFundPayments();
    current.push(payment);
    this.writeStorage(CashBalanceStep4Service.STORAGE_PAYMENTS_KEY, JSON.stringify(current));

    return this.http
      .post<{ id: number }>(`${this.baseUrl}/savings-fund/payments`, {
        date: fecha,
        amount: valor,
        description: concepto
      })
      .pipe(
        map(res => ({ ...payment, id: res?.id ? String(res.id) : payment.id })),
        catchError(() => of(payment))
      );
  }

  removeFundPayment(id: string): void {
    const filtered = this.getStoredFundPayments().filter(p => p.id !== id);
    this.writeStorage(CashBalanceStep4Service.STORAGE_PAYMENTS_KEY, JSON.stringify(filtered));
  }

  /**
   * Estado del fondo al cierre del día operativo 'fecha' (fiel a fondoArriendoAl de store.js)
   */
  fondoArriendoAl(fecha: string): SavingsFundState {
    const ahorro = this.AHORRO_DIARIO_VIGENTE;
    const dias = Math.max(0, this.diasEntreFechas(this.FONDO_INICIO, fecha) + 1);
    const allPayments = this.getStoredFundPayments();
    const pagosHasta = allPayments.filter(p => p.fecha <= fecha);
    const pagosHoy = pagosHasta.filter(p => p.fecha === fecha);

    const ahorrado = dias * ahorro;
    const totalPagosHasta = pagosHasta.reduce((s, p) => s + p.valor, 0);
    const acumulado = ahorrado - totalPagosHasta;
    const ahorroHoy = dias > 0 ? ahorro : 0;
    const totalPagosHoy = pagosHoy.reduce((s, p) => s + p.valor, 0);

    const ultimoPago = pagosHasta
      .filter(p => p.fecha < fecha)
      .map(p => p.fecha)
      .sort()
      .pop();
    const desde = ultimoPago ? this.diasEntreFechas(this.FONDO_INICIO, ultimoPago) + 1 : 0;
    const [y, m, d] = this.FONDO_INICIO.split('-').map(Number);
    const lista: number[] = [];
    for (let i = desde; i < dias; i++) {
      lista.push(new Date(y, m - 1, d + i).getDate());
    }

    return {
      ahorroDiario: ahorro,
      dias,
      acumulado,
      saldoAnterior: acumulado - ahorroHoy + totalPagosHoy,
      ahorroHoy,
      pagosHoy,
      totalPagosHoy,
      diasLista: lista
    };
  }

  /**
   * Obtiene los datos remotos de paso 2 y gastos del día si el backend está disponible
   */
  fetchDayContext(fecha: string): Observable<{
    vendorCloses: VendorCloseSummaryDto[];
    backendExpenses: DailyExpenseItem[] | null;
    backendStep4: BackendCashBalanceStepResponseDto | null;
  }> {
    const params = new HttpParams().set('date', fecha);
    const vendorCloses$ = this.http
      .get<VendorCloseSummaryDto[]>(`${this.baseUrl}/vendor-closes`, { params })
      .pipe(catchError(() => of([])));

    const expenses$ = this.http
      .get<{ expenses: { id: number; date: string; amount: number; description: string }[]; total: number }>(
        `${this.baseUrl}/administrative-expenses`,
        { params }
      )
      .pipe(
        map(res =>
          (res.expenses ?? []).map(e => ({
            id: e.id,
            date: e.date,
            razonSocial: e.description.includes('·') ? e.description.split('·')[0].trim() : e.description,
            concepto: e.description.includes('·') ? e.description.split('·').slice(1).join('·').trim() : 'Gasto administrativo',
            valor: Number(e.amount)
          }))
        ),
        catchError(() => of(null))
      );

    const step4$ = this.http
      .get<BackendCashBalanceStepResponseDto>(`${this.baseUrl}/cash-balance/${fecha}`)
      .pipe(catchError(() => of(null)));

    return forkJoin({
      vendorCloses: vendorCloses$,
      backendExpenses: expenses$,
      backendStep4: step4$
    });
  }

  saveDominusBalanceToBackend(fecha: string, accumulatedBalance: number): Observable<BackendCashBalanceStepResponseDto | null> {
    return this.http
      .put<BackendCashBalanceStepResponseDto>(`${this.baseUrl}/cash-balance/${fecha}/dominus`, {
        accumulatedBalance
      })
      .pipe(catchError(() => of(null)));
  }

  /**
   * Todas las cifras de la tabla 4c (fiel a jrP4Calculo de jornada-paso-4.js):
   *   saldo de efectivo = saldo anterior + (efectivo del día − ahorro diario) − consignaciones
   *   saldo justificado = saldo de efectivo + ahorro acumulado + recibos
   *   diferencia = saldo Dominus − saldo justificado
   */
  calculateStep4(
    fecha: string,
    options: {
      journalConsecutive?: number | null;
      vendorCloses?: VendorCloseSummaryDto[];
      backendExpenses?: DailyExpenseItem[] | null;
    } = {}
  ): Step4CalculationResult {
    const datos = this.getCapturedData(fecha);
    const semilla = this.saldoCajaSeed.find(s => s.date === fecha);
    const fondo = this.fondoArriendoAl(fecha);

    // Efectivo del día y total estación (tirilla): paso 2 en vivo o cierres históricos
    const closes = options.vendorCloses ?? [];
    const tieneP2 = closes.length > 0;
    const hist = this.historicalDayTotals[fecha];

    let dinero = 0;
    let origenDinero = 'sin cierre en el paso 2';
    let totalEstacion: number | null = null;

    if (tieneP2) {
      dinero = closes.reduce((sum, c) => sum + (Number(c.cash) || 0), 0);
      totalEstacion = closes.reduce((sum, c) => sum + (Number(c.dominusTirilla) || 0), 0);
      origenDinero = 'efectivo del día · paso 2';
    } else if (hist) {
      dinero = hist.efectivo;
      totalEstacion = hist.tirilla;
      origenDinero = 'cierres registrados de la fecha';
    }

    // Gastos del día (4b)
    const seedExpenses = this.adminExpensesSeed.filter(g => g.date === fecha);
    const gastos =
      options.backendExpenses && options.backendExpenses.length > 0 ? options.backendExpenses : seedExpenses;
    const recibos = semilla ? semilla.recibosGA : gastos.reduce((s, g) => s + g.valor, 0);

    // Saldo anterior: planilla histórica · saldo del día anterior · o digitado
    const prevDate = this.previousDateIso(fecha);
    const prevSemilla = this.saldoCajaSeed.find(s => s.date === prevDate);
    const prevCaptured = this.getCapturedData(prevDate);

    let saldoAnterior: number | null = null;
    let origenAnterior: 'planilla histórica' | 'saldo del día anterior' | 'digitar' = 'digitar';

    if (semilla) {
      saldoAnterior = semilla.dineroAnterior;
      origenAnterior = 'planilla histórica';
    } else if (prevSemilla) {
      // Si el día anterior fue semilla, podemos calcular su saldo efectivo
      const prevHist = this.historicalDayTotals[prevDate];
      const prevDinero = prevHist ? prevHist.efectivo : 0;
      const prevFondo = this.fondoArriendoAl(prevDate);
      const prevConsig = prevSemilla.consignaciones;
      saldoAnterior = prevSemilla.dineroAnterior + (prevDinero - prevFondo.ahorroHoy) - prevConsig;
      origenAnterior = 'saldo del día anterior';
    } else if (typeof prevCaptured.saldoAnterior === 'number') {
      const prevHist = this.historicalDayTotals[prevDate];
      const prevDinero = prevHist ? prevHist.efectivo : 0;
      const prevFondo = this.fondoArriendoAl(prevDate);
      const prevConsig = this.consignacionesSeed.filter(m => m.date === prevDate).reduce((s, m) => s + m.amount, 0);
      saldoAnterior = prevCaptured.saldoAnterior + (prevDinero - prevFondo.ahorroHoy) - prevConsig;
      origenAnterior = 'saldo del día anterior';
    } else if (typeof datos.saldoAnterior === 'number') {
      saldoAnterior = datos.saldoAnterior;
      origenAnterior = 'digitar';
    }

    const consignaciones = semilla
      ? semilla.consignaciones
      : this.consignacionesSeed.filter(m => m.date === fecha).reduce((s, m) => s + m.amount, 0);

    const saldoEfectivo =
      saldoAnterior === null ? null : saldoAnterior + (dinero - fondo.ahorroHoy) - consignaciones;
    const saldoJustificado =
      saldoEfectivo === null ? null : saldoEfectivo + fondo.acumulado + recibos;

    const saldoDominus =
      typeof datos.saldoDominus === 'number'
        ? datos.saldoDominus
        : semilla
          ? semilla.saldoDominus
          : null;

    const diferencia =
      saldoJustificado === null || saldoDominus === null
        ? null
        : Math.round((saldoDominus - saldoJustificado) * 100) / 100;

    const totalEnCaja = saldoEfectivo === null ? null : saldoEfectivo + fondo.acumulado;

    const consolidado = typeof datos.consolidadoDominus === 'number' ? datos.consolidadoDominus : null;
    const cuadre: ConsolidadoCuadreState =
      consolidado === null || totalEstacion === null
        ? 'pendiente'
        : Math.abs(consolidado - totalEstacion) <= this.TOLERANCIA_CONSOLIDADO
          ? 'cuadra'
          : 'difiere';

    return {
      planilla: this.planillaForDate(fecha, options.journalConsecutive),
      dinero,
      origenDinero,
      fondo,
      gastos,
      recibos,
      semilla: !!semilla,
      saldoAnterior,
      origenAnterior,
      consignaciones,
      saldoEfectivo,
      saldoJustificado,
      saldoDominus,
      saldoDominusPegado: typeof datos.saldoDominus === 'number',
      diferencia,
      totalEnCaja,
      totalEstacion,
      consolidado,
      cuadre
    };
  }

  setPdfInMemory(fecha: string, file: File, bytes: Uint8Array): void {
    this.pdfMemoryStore[fecha] = { name: file.name, bytes, file };
    this.saveCapturedData(fecha, {
      pdf: {
        nombre: file.name,
        tamano: file.size,
        cargadoEn: new Date().toISOString()
      }
    });
  }

  getPdfInMemory(fecha: string): { name: string; bytes: Uint8Array; file: File; finalUrl?: string } | null {
    return this.pdfMemoryStore[fecha] ?? null;
  }

  clearPdfInMemory(fecha: string): void {
    delete this.pdfMemoryStore[fecha];
    this.saveCapturedData(fecha, { pdf: null });
  }
}

