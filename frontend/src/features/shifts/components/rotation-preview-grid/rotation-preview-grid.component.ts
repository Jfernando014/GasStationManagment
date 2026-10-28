import { ChangeDetectionStrategy, Component, ElementRef, afterRenderEffect, computed, inject, input } from '@angular/core';

import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import { dayOfMonth, isSunday, prefersReducedMotion, shortDateLabel, weekdayShort } from '../../../../shared/helpers/shift-display.helpers';
import {
  RotationCoverage,
  RotationPreviewResponse,
  RotationRowStatus
} from '../../../../shared/models/dtos/shift-rotation.dto';

interface PreviewCell {
  code: string;
  status: RotationRowStatus;
  label: string;
}

interface PreviewRow {
  workerId: string;
  name: string;
  cells: (PreviewCell | null)[];
}

interface PreviewColumn {
  date: string;
  day: number;
  weekday: string;
  sunday: boolean;
  skipped: boolean;
  coverage: RotationCoverage;
  coverageTip: string;
}

const STATUS_TIPS: Record<RotationRowStatus, string> = {
  NEW: 'se escribe',
  REGENERATED: 'se vuelve a generar',
  REPLACED_MANUAL: 'estaba cambiado a mano: se sobrescribe',
  KEPT_MANUAL: 'cambiado a mano: se conserva',
  SKIPPED_NOVELTY: 'tiene novedades del personal del día: no se toca',
  UNCHANGED: 'ya tenía este turno'
};

/** "mié 05/11: sin titular de noche; horas sin nadie: 12:00 a 18:00" */
export function rotationCoverageTip(date: string, c: RotationCoverage): string {
  if (c.covered) return `${shortDateLabel(date)}: cobertura completa`;
  const missing: string[] = [];
  if (!c.dayCovered) missing.push('sin titular de día');
  if (!c.nightCovered) missing.push('sin titular de noche');
  if (!c.supportCovered) missing.push('sin apoyo');
  if (c.uncoveredHours.length) missing.push(`horas sin nadie: ${c.uncoveredHours.join(', ')}`);
  return `${shortDateLabel(date)}: ${missing.join('; ')}`;
}

/**
 * Malla propuesta por la vista previa, sin guardar. Las celdas se encienden en orden, día por día, para que se vea el
 * ritmo del ciclo; las omitidas por novedades quedan atenuadas con su icono y su motivo.
 */
@Component({
  selector: 'app-rotation-preview-grid',
  standalone: true,
  imports: [ShiftChipComponent],
  templateUrl: './rotation-preview-grid.component.html',
  styleUrl: './rotation-preview-grid.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RotationPreviewGridComponent {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly preview = input.required<RotationPreviewResponse>();

  constructor() {
    // Cada vista previa nueva vuelve a encender las celdas en orden, sin redibujar la tabla
    afterRenderEffect(() => {
      this.preview();
      if (prefersReducedMotion()) return;
      this.host.nativeElement.getAnimations({ subtree: true }).forEach(animation => {
        animation.cancel();
        animation.play();
      });
    });
  }

  readonly columns = computed<PreviewColumn[]>(() => {
    const preview = this.preview();
    const skipped = new Set(preview.skippedDates);
    return preview.days.map(d => ({
      date: d.date,
      day: dayOfMonth(d.date),
      weekday: weekdayShort(d.date),
      sunday: isSunday(d.date),
      skipped: skipped.has(d.date),
      coverage: d.coverage,
      coverageTip: rotationCoverageTip(d.date, d.coverage)
    }));
  });

  readonly rows = computed<PreviewRow[]>(() => {
    const preview = this.preview();
    const order = new Map<string, string>();
    preview.phases.forEach(p => order.set(p.workerId, p.fullName));
    preview.days.forEach(d => d.rows.forEach(r => { if (!order.has(r.workerId)) order.set(r.workerId, r.fullName); }));
    return [...order.entries()].map(([workerId, name]) => ({
      workerId,
      name,
      cells: preview.days.map(d => {
        const row = d.rows.find(r => r.workerId === workerId);
        return row
          ? { code: row.shiftCode, status: row.status, label: `${name}, ${shortDateLabel(d.date)}: ${row.shiftCode}, ${STATUS_TIPS[row.status]}` }
          : null;
      })
    }));
  });

  delay(rowIndex: number, colIndex: number): string {
    return `${colIndex * 28 + rowIndex * 40}ms`;
  }
}
