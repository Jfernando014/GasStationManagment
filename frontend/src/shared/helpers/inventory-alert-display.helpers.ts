import { AlertStatus, AutonomyLevel } from '../models/dtos/inventory-alert.dto';

/** Level of autonomy as the screens show it. */
export function autonomyLevelLabel(level: AutonomyLevel | null): string {
  if (level === 'CRITICAL') return 'Crítico';
  if (level === 'WARNING') return 'Advertencia';
  if (level === 'OK') return 'Normal';
  return 'Sin evaluar';
}

/** Variant of `app-tag` for an autonomy level. */
export function autonomyLevelTone(level: AutonomyLevel | null): 'green' | 'amber' | 'red' | 'gray' {
  if (level === 'CRITICAL') return 'red';
  if (level === 'WARNING') return 'amber';
  if (level === 'OK') return 'green';
  return 'gray';
}

export function alertStatusLabel(status: AlertStatus): string {
  if (status === 'ACTIVE') return 'Activa';
  if (status === 'READ') return 'Leída';
  if (status === 'ATTENDED') return 'Atendida';
  return 'Resuelta';
}

/** Variant of `app-tag` for an alert status. */
export function alertStatusTone(status: AlertStatus): 'green' | 'amber' | 'red' | 'gray' {
  if (status === 'ACTIVE') return 'red';
  if (status === 'READ') return 'amber';
  if (status === 'ATTENDED') return 'gray';
  return 'green';
}

export function alertTypeLabel(type: string): string {
  return type === 'AUTONOMY' ? 'Autonomía' : type;
}
