/** Level of a variation alert; OK means that there is no alert. */
export type AlertLevel = 'OK' | 'REVISAR' | 'AYUDA' | 'ALARMA';

/** A level as it is shown on screen, where there may be nothing to judge. */
export type ShownAlertLevel = AlertLevel | 'SIN_DATOS';

export const ALERT_LEVEL_LABELS: Record<ShownAlertLevel, string> = {
  OK: 'Bien',
  REVISAR: 'Revisar',
  AYUDA: 'Pedir ayuda',
  ALARMA: 'Alarma',
  SIN_DATOS: 'Sin datos'
};
