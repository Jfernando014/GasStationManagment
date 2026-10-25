import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import {
  AssignedVendorDto,
  CatalogProductItem,
  DaySummaryDto,
  EffectiveDiscrepancyToleranceDto,
  VendorCloseSummaryDto,
  VendorShiftCloseBatchRequestDto
} from '../../shared/models/dtos/vendor-close.dto';

@Injectable({
  providedIn: 'root'
})
export class SalesVendorCloseService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/v1/sales/vendor-closes';
  private readonly toleranceUrl = 'http://localhost:8080/api/v1/sales/discrepancy-tolerances/effective';

  getAssignedStaff(date: string): Observable<AssignedVendorDto[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<AssignedVendorDto[]>(`${this.baseUrl}/assigned-staff`, { params });
  }

  getVendorCloses(date: string): Observable<VendorCloseSummaryDto[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<VendorCloseSummaryDto[]>(this.baseUrl, { params });
  }

  getDaySummary(date: string): Observable<DaySummaryDto> {
    const params = new HttpParams().set('date', date);
    return this.http.get<DaySummaryDto>(`${this.baseUrl}/day-summary`, { params });
  }

  getEffectiveTolerance(date: string): Observable<EffectiveDiscrepancyToleranceDto> {
    const params = new HttpParams().set('date', date);
    return this.http.get<EffectiveDiscrepancyToleranceDto>(this.toleranceUrl, { params });
  }

  saveVendorCloses(request: VendorShiftCloseBatchRequestDto): Observable<VendorCloseSummaryDto[]> {
    const params = new HttpParams().set('date', request.journalDate);
    return this.http.post<VendorCloseSummaryDto[]>(this.baseUrl, request, { params });
  }

  /**
   * Catalog of authentic EDS Bellavista products for basket and additives
   */
  getCatalogProducts(): CatalogProductItem[] {
    return [
      { sku: '76102', name: 'Aditivo Gasolina Ruta 75 40ml', category: 'Aditivos', price: 6800.0 },
      { sku: '75089', name: 'Super Concentrado 4x1 Plus 40ml', category: 'Aditivos', price: 7200.0 },
      { sku: '76101', name: 'Elevador de Octanaje Ruta 75 237ml', category: 'Aditivos', price: 17300.0 },
      { sku: '74063', name: 'Refrigerante Corriente Petrocom Cuarto', category: 'Refrigerantes', price: 6000.0 },
      { sku: '74039', name: 'Agua para Batería Petrocom', category: 'Refrigerantes', price: 2500.0 },
      { sku: '74066', name: 'Líquido de Frenos DOT 3 Petrocom', category: 'Varios', price: 8000.0 },
      { sku: '80216', name: 'Terpel Celerity Bio Antihumo Cuarto', category: 'Lubricantes', price: 28500.0 },
      { sku: '80262', name: 'Terpel Celerity Bio Antihumo 2T Pinta', category: 'Lubricantes', price: 15500.0 },
      { sku: '80266', name: 'Mobil Súper Moto 4T Cuarto', category: 'Lubricantes', price: 34000.0 },
      { sku: '80271', name: 'Mobil Súper Moto 4T Pinta', category: 'Lubricantes', price: 19000.0 },
      { sku: '80200', name: 'Terpel Celerity 4T Cuarto', category: 'Lubricantes', price: 28000.0 },
      { sku: '80204', name: 'Terpel Celerity 4T Pinta', category: 'Lubricantes', price: 15000.0 },
      { sku: '80210', name: 'Terpel Oiltec 20W50 Cuarto Multigrado', category: 'Lubricantes', price: 29000.0 },
      { sku: '80070', name: 'Mobil Special HD50 Cuarto', category: 'Lubricantes', price: 30000.0 },
      { sku: '80171', name: 'Mobil Súper 1000 20W50 Cuarto', category: 'Lubricantes', price: 42000.0 },
      { sku: '80258', name: 'Mobil Hidráulico AW68 Cuarto', category: 'Lubricantes', price: 22000.0 },
      { sku: '76001', name: 'Bolsas para Combustible', category: 'Lubricantes', price: 5000.0 },
      { sku: '80190', name: 'Aditivo para acpm (474 tarro negro)', category: 'Aditivos', price: 6500.0 },
      { sku: '75005', name: 'Aditivo Diesel Ruta 75 118ml', category: 'Aditivos', price: 13400.0 }
    ];
  }

  /**
   * Authentic August 5, 2026 calibration data (Control Juan Clavijo Hoja 05)
   */
  getCalibrationDemo0805() {
    return {
      fecha: '2026-08-05',
      vendedores: [
        {
          document: '52875796',
          name: 'Mónica Dorado',
          role: 'ISLERO',
          shiftName: '12-7',
          pumpNumber: 3,
          workerCode: 'MON-127'
        },
        {
          document: '1061696674',
          name: 'Carlos Andrés Paredes',
          role: 'ISLERO',
          shiftName: 'DIA6',
          pumpNumber: 1,
          workerCode: 'CAR-D6'
        },
        {
          document: '76326816',
          name: 'Wiltom Andrés Paz',
          role: 'ISLERO',
          shiftName: 'NOCHE',
          pumpNumber: 1,
          workerCode: 'WIL-NOC'
        }
      ],
      capturas: {
        '52875796': {
          b100k: 8,
          b50k: 24,
          b20k: 7,
          b10k: 14,
          b5k: 13,
          b2k: 14,
          parciales: 10,
          pico: 373000,
          ga: 0,
          credibanco: 1354436,
          qr: 162606,
          redenciones: 0,
          galMotor: 223.701,
          galDiesel: 13.413,
          galExtra: 2.755,
          galMaxpro: 0,
          canastilla: 20800,
          tirilla: 3890028.87
        },
        '1061696674': {
          b100k: 13,
          b50k: 26,
          b20k: 0,
          b10k: 2,
          b5k: 2,
          b2k: 24,
          parciales: 9,
          pico: 47000,
          ga: 0,
          credibanco: 2019760,
          qr: 377902,
          redenciones: 41380,
          galMotor: 229.649,
          galDiesel: 11.314,
          galExtra: 13.606,
          galMaxpro: 7.595,
          canastilla: 25800,
          tirilla: 4285510.58
        },
        '76326816': {
          b100k: 9,
          b50k: 29,
          b20k: 21,
          b10k: 44,
          b5k: 10,
          b2k: 25,
          parciales: 16,
          pico: 110000,
          ga: 0,
          credibanco: 2317116,
          qr: 335500,
          redenciones: 0,
          galMotor: 331.237,
          galDiesel: 33.753,
          galExtra: 0.735,
          galMaxpro: 7.820,
          canastilla: 34500,
          tirilla: 5961277.95
        }
      },
      productos: {
        '76102': { '52875796': 2, '1061696674': 3, '76326816': 1 },
        '75089': { '76326816': 2 },
        '74063': { '1061696674': 1 },
        '80266': { '1061696674': 1 }
      }
    };
  }
}
