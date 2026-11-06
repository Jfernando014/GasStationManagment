import {
  AssignmentStatus,
  CredibancoDayResult,
  ElectronicPaymentsStatus,
  ElectronicVendorRow,
  PointsResult,
  QrReconciliationResult,
  VoucherRequest
} from '../models/dtos/electronic-payments.dto';
import { formatCop, formatInputDate } from './format.helpers';

/** Estado de una pestaña: verde (cuadra), ámbar (falta información) o rojo (no cuadra). */
export type Tone = 'ok' | 'aviso' | 'error';

/** Tope de dígitos que acepta el backend: 12 enteros y 2 decimales. */
const MAX_AMOUNT = 999_999_999_999.99;

/** Tamaño máximo del CSV: el mismo que spring.servlet.multipart.max-file-size. */
export const MAX_REDEBAN_BYTES = 25 * 1024 * 1024;

/**
 * Convierte lo que se digita en un importe. Formato colombiano: el punto separa miles y la coma los decimales.
 * "1.354.436" -> 1354436, "1354436,5" -> 1354436.5, "" -> 0. Devuelve null si no es un importe válido
 * (letras, negativos, más de 2 decimales).
 */
export function parseAmount(text: string): number | null {
  const cleaned = text.replace(/[\s$]/g, '');
  if (cleaned === '') return 0;

  let normalized: string;
  if (cleaned.includes(',')) {
    if (!/^(\d{1,3}(\.\d{3})+|\d+)(,\d{1,2})?$/.test(cleaned)) return null;
    normalized = cleaned.replace(/\./g, '').replace(',', '.');
  } else if (/^\d{1,3}(\.\d{3})+$/.test(cleaned)) {
    normalized = cleaned.replace(/\./g, '');
  } else if (/^\d+(\.\d{1,2})?$/.test(cleaned)) {
    normalized = cleaned;
  } else {
    return null;
  }

  const value = Number(normalized);
  return Number.isFinite(value) && value >= 0 && value <= MAX_AMOUNT ? value : null;
}

/** Importe para un campo de texto: sin símbolo, con puntos de miles. El cero se deja vacío. */
export function formatAmount(value: number): string {
  if (!value) return '';
  const [integer, fraction] = value.toFixed(2).split('.');
  const grouped = integer.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return fraction === '00' ? grouped : `${grouped},${fraction}`;
}

/** Diferencia con signo: "+$1.200", "-$500" o "$0". */
export function formatSignedCop(value: number): string {
  if (Math.round(value) === 0) return '$0';
  return value > 0 ? `+${formatCop(value)}` : formatCop(value);
}

/** Los seis importes que se envían al servidor, tomados de una fila. */
export function voucherValues(row: ElectronicVendorRow): VoucherRequest {
  return {
    mastercard: row.mastercard,
    visa: row.visa,
    amex: row.amex,
    mastercardDawn: row.mastercardDawn,
    visaDawn: row.visaDawn,
    amexDawn: row.amexDawn
  };
}

export function credibancoTone(result: CredibancoDayResult): Tone {
  if (result.vendors.length === 0) return 'aviso';
  return result.allMatch ? 'ok' : 'error';
}

/** Sin CSV y sin QR en las planillas no hay nada que comparar: la pestaña está en orden. */
export function qrTone(result: QrReconciliationResult): Tone {
  if (!result.redebanLoaded) return result.planillaTotal === 0 ? 'ok' : 'aviso';
  if (!result.matchesDay) return 'error';
  if (!result.perVendorReliable) return 'aviso';
  return result.vendors.every(vendor => vendor.matches) ? 'ok' : 'error';
}

export function pointsTone(result: PointsResult): Tone {
  switch (result.status) {
    case 'COMPLETED_ZERO':
    case 'MATCH':
      return 'ok';
    case 'PENDING_TOTAL':
      return 'aviso';
    default:
      return 'error';
  }
}

/**
 * Un vendedor cuadra cuando sus comprobantes igualan el Credibanco de su planilla y su QR también:
 * con Redeban cargado se compara contra Redeban; sin él, solo cuadra si no declaró QR.
 */
export function vendorMatches(status: ElectronicPaymentsStatus, workerId: string): boolean {
  const credibanco = status.credibanco.vendors.find(vendor => vendor.workerId === workerId);
  if (!credibanco?.matches) return false;
  if (!status.qr.redebanLoaded) {
    return (status.vendors.find(vendor => vendor.workerId === workerId)?.planillaQr ?? 0) === 0;
  }
  return status.qr.vendors.find(vendor => vendor.workerId === workerId)?.matches ?? false;
}

export function countMatchingVendors(status: ElectronicPaymentsStatus): number {
  return status.vendors.filter(vendor => vendorMatches(status, vendor.workerId)).length;
}

const ASSIGNMENT_REASONS: Record<AssignmentStatus, string> = {
  ASSIGNED: 'Asignada',
  NO_TIME: 'Sin hora en el archivo',
  NO_SHIFT: 'Ningún turno cubre esa hora',
  AMBIGUOUS: 'Varios vendedores a esa hora'
};

export function assignmentReason(status: AssignmentStatus): string {
  return ASSIGNMENT_REASONS[status] ?? status;
}

/** Hora "HH:mm:ss" del backend como "HH:mm". */
export function formatTime(time: string | null): string {
  return time ? time.slice(0, 5) : '—';
}

/** Nombre para mostrar de un vendedor, con el turno cuando se conoce. */
export function vendorLabel(name: string | null, shiftCode: string | null): string {
  const base = name?.trim() || 'Vendedor sin nombre';
  return shiftCode ? `${base} (${shiftCode})` : base;
}

/** Motivo por el que un archivo no se puede subir, o null si es válido. */
export function validateRedebanFile(file: { name: string; size: number }): string | null {
  if (!/\.csv$/i.test(file.name)) return 'El archivo de Redeban debe ser un CSV (.csv).';
  if (file.size === 0) return 'El archivo está vacío.';
  if (file.size > MAX_REDEBAN_BYTES) return 'El archivo supera los 25 MB permitidos.';
  return null;
}

/** Texto para pegar en WhatsApp con el estado del paso 3 de todos los vendedores. */
export function buildStepSummary(status: ElectronicPaymentsStatus): string {
  const lines: string[] = [`*Medios electrónicos ${formatInputDate(status.date)}*`];

  lines.push('', `Credibanco: ${status.credibanco.allMatch ? 'Cuadra' : 'No cuadra'}`);
  for (const check of status.credibanco.vendors) {
    const detail = check.matches
      ? 'Cuadra'
      : `No cuadra (comprobantes ${formatCop(check.vouchersTotal)}, planilla ${formatCop(check.planillaCredibanco)})`;
    lines.push(`- ${vendorLabel(check.workerName, check.shiftCode)}: ${detail}`);
  }

  lines.push('');
  if (!status.qr.redebanLoaded) {
    lines.push(
      status.qr.planillaTotal === 0 ? 'QR: sin movimiento' : 'QR: pendiente de cargar el archivo de Redeban'
    );
  } else {
    lines.push(`QR: ${status.qr.matchesDay ? 'Cuadra' : 'No cuadra'} (Redeban ${formatCop(status.qr.redebanTotal)}, planillas ${formatCop(status.qr.planillaTotal)})`);
    for (const check of status.qr.vendors) {
      const detail = check.matches ? 'Cuadra' : `No cuadra (${formatSignedCop(check.difference)})`;
      lines.push(`- ${vendorLabel(check.workerName, check.shiftCode)}: ${detail}`);
    }
    if (!status.qr.perVendorReliable) {
      lines.push('Hay transacciones sin asignar a un vendedor.');
    }
  }

  lines.push('');
  switch (status.points.status) {
    case 'COMPLETED_ZERO':
      lines.push('Puntos Colombia: sin redenciones');
      break;
    case 'PENDING_TOTAL':
      lines.push(`Puntos Colombia: pendiente el total (redenciones ${formatCop(status.points.declared)})`);
      break;
    case 'MATCH':
      lines.push(`Puntos Colombia: Cuadra (${formatCop(status.points.declared)})`);
      break;
    default:
      lines.push(
        `Puntos Colombia: No cuadra (redenciones ${formatCop(status.points.declared)}, total ${formatCop(status.points.pointsTotal ?? 0)})`
      );
  }

  lines.push('', status.canComplete ? 'Paso 3: todo cuadra.' : 'Paso 3: pendiente.');
  return lines.join('\n');
}
