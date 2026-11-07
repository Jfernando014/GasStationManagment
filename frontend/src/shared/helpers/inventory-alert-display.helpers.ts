import { AlertStatus, AutonomyLevel } from '../models/dtos/inventory-alert.dto';

/** Level of autonomy as the screens show it. */
export function autonomyLevelLabel(level: AutonomyLevel | null): string {
  if (level === 'CRITICAL') return 'Crítico';
  if (level === 'WARNING') return 'Advertencia';
  if (level === 'OK') return 'Normal';
  return 'Sin evaluar';
}

/** Tone of the `state-pill-tag` class for an autonomy level. */
export function autonomyLevelTone(level: AutonomyLevel | null): 'optimal' | 'warning' | 'critical' | 'neutral' {
  if (level === 'CRITICAL') return 'critical';
  if (level === 'WARNING') return 'warning';
  if (level === 'OK') return 'optimal';
  return 'neutral';
}

export function alertStatusLabel(status: AlertStatus): string {
  if (status === 'ACTIVE') return 'Activa';
  if (status === 'READ') return 'Leída';
  if (status === 'ATTENDED') return 'Atendida';
  return 'Resuelta';
}

/** Tone of the `state-pill-tag` class for an alert status. */
export function alertStatusTone(status: AlertStatus): 'optimal' | 'warning' | 'critical' | 'neutral' {
  if (status === 'ACTIVE') return 'critical';
  if (status === 'READ') return 'warning';
  if (status === 'ATTENDED') return 'neutral';
  return 'optimal';
}

export function alertTypeLabel(type: string): string {
  return type === 'AUTONOMY' ? 'Autonomía' : type;
}
