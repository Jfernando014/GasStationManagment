import { ChangeDetectionStrategy, Component, ElementRef, computed, input, output, viewChildren } from '@angular/core';

import { formatGallons, FUEL_COLORS, FUEL_LABELS } from '../../../../shared/helpers/inventory-display.helpers';
import { isValidTankHeight } from '../../../../shared/helpers/inventory-rules.helpers';
import { TankResponse } from '../../../../shared/models/dtos/tank.dto';
import { TankHeightChange } from '../../models/step5.model';

/** Step 5a: the six ruler readings of the day and the gallons calculated by the server. */
@Component({
  selector: 'app-tank-measurements',
  standalone: true,
  templateUrl: './tank-measurements.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TankMeasurementsComponent {
  private readonly heightInputs = viewChildren<ElementRef<HTMLInputElement>>('heightInput');

  readonly tanks = input.required<TankResponse[]>();
  readonly heights = input.required<Record<number, number | null>>();
  readonly gallons = input.required<Record<number, number | null>>();
  readonly editable = input(true);
  readonly saving = input(false);

  readonly heightChanged = output<TankHeightChange>();
  readonly heightsPasted = output<Record<number, number | null>>();
  readonly saveRequested = output<void>();

  readonly fuelLabels = FUEL_LABELS;
  readonly fuelColors = FUEL_COLORS;
  readonly formatGallons = formatGallons;

  readonly measuredCount = computed(() =>
    this.tanks().filter(tank => isValidTankHeight(tank, this.heights()[tank.id])).length
  );
  readonly allMeasured = computed(() =>
    this.tanks().length > 0 && this.measuredCount() === this.tanks().length
  );

  heightText(tankId: number): string {
    const height = this.heights()[tankId];
    return height === null || height === undefined ? '' : String(height).replace('.', ',');
  }

  isHeightInvalid(tank: TankResponse): boolean {
    const height = this.heights()[tank.id];
    return height !== null && height !== undefined && !isValidTankHeight(tank, height);
  }

  fillPercent(tank: TankResponse): string {
    const gallons = this.gallons()[tank.id];
    if (gallons === null || gallons === undefined || tank.maxCapacityGallons <= 0) return '—';
    return `${formatGallons((gallons / tank.maxCapacityGallons) * 100, 1)} %`;
  }

  onHeightInput(tank: TankResponse, event: Event): void {
    if (!this.editable()) return;
    const raw = (event.target as HTMLInputElement).value.replace(',', '.');
    const height = raw.trim() === '' ? null : Number(raw);
    this.heightChanged.emit({
      tankId: tank.id,
      height: height !== null && Number.isFinite(height) ? height : null
    });
  }

  onHeightKeydown(event: KeyboardEvent, index: number): void {
    if (event.key !== 'Enter' && event.key !== 'ArrowDown' && event.key !== 'ArrowUp') return;
    event.preventDefault();
    const offset = event.key === 'ArrowUp' || (event.key === 'Enter' && event.shiftKey) ? -1 : 1;
    const next = this.heightInputs()[index + offset]?.nativeElement;
    if (next) {
      next.focus();
      next.select();
    } else if (event.key === 'Enter') {
      (event.target as HTMLInputElement).blur();
    }
  }

  /** Pasting the six heights one per line fills the column from the focused row down. */
  onHeightsPaste(event: ClipboardEvent, index: number): void {
    if (!this.editable()) return;
    const text = event.clipboardData?.getData('text') ?? '';
    const parts = text.split(/\r?\n|\t/).map(part => part.trim()).filter(part => part !== '');
    if (parts.length <= 1) return;
    event.preventDefault();

    const tanks = this.tanks();
    const heights: Record<number, number | null> = {};
    parts.forEach((part, offset) => {
      const tank = tanks[index + offset];
      if (!tank) return;
      const height = Number(part.replace(',', '.'));
      if (!Number.isFinite(height)) return;
      heights[tank.id] = height;
    });
    this.heightsPasted.emit(heights);
  }
}
