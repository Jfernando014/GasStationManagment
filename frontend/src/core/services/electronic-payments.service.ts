import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  ElectronicPaymentsStatus,
  PointsResult,
  RedebanImportResponse,
  VoucherRequest
} from '../../shared/models/dtos/electronic-payments.dto';

/**
 * Paso 3 de la jornada: Credibanco, QR (Redeban) y Puntos Colombia.
 * Los errores llegan ya convertidos en ApiError por el interceptor.
 */
@Injectable({ providedIn: 'root' })
export class ElectronicPaymentsService {
  private readonly http = inject(HttpClient);
  private readonly salesUrl = 'http://localhost:8080/api/v1/sales';
  private readonly paymentsUrl = `${this.salesUrl}/electronic-payments`;
  private readonly reconciliationUrl = `${this.salesUrl}/reconciliation`;

  /** Estado completo del paso: vendedores, las tres conciliaciones y lo que impide completarlo. */
  getStatus(date: string): Observable<ElectronicPaymentsStatus> {
    return this.http.get<ElectronicPaymentsStatus>(`${this.paymentsUrl}/${date}`);
  }

  /** Guarda los comprobantes de un vendedor; la planilla se toma del paso 2 en el servidor. */
  saveVouchers(date: string, workerId: string, body: VoucherRequest): Observable<ElectronicPaymentsStatus> {
    return this.http.put<ElectronicPaymentsStatus>(`${this.paymentsUrl}/${date}/vendors/${workerId}/vouchers`, body);
  }

  /** Sube el CSV de Redeban del día (campo multipart "file"). */
  importRedeban(date: string, file: File): Observable<RedebanImportResponse> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<RedebanImportResponse>(`${this.reconciliationUrl}/qr/${date}/redeban`, form);
  }

  savePoints(date: string, total: number): Observable<PointsResult> {
    return this.http.put<PointsResult>(`${this.reconciliationUrl}/points/${date}`, { total });
  }

  /** Texto de WhatsApp del QR de un vendedor. Exige el CSV de Redeban cargado. */
  getQrSummary(date: string, workerId: string): Observable<{ text: string }> {
    return this.http.get<{ text: string }>(`${this.reconciliationUrl}/qr/${date}/summary/${workerId}`);
  }
}
