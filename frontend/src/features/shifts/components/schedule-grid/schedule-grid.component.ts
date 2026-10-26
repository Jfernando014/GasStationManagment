import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  Output,
  computed,
  effect,
  inject,
  input,
  signal,
  afterNextRender,
  DestroyRef
} from '@angular/core';

import { AvatarComponent } from '../../../../atomic-design/atoms/avatar/avatar.component';
import { ShiftChipComponent } from '../../../../atomic-design/atoms/shift-chip/shift-chip.component';
import {
  dayOfMonth,
  shortName,
  isSunday,
  longDateLabel,
  shortDateLabel,
  weekdayShort,
  shiftTone,
  ShiftTone
} from '../../../../shared/helpers/shift-display.helpers';
import { DailyCoverage } from '../../../../shared/models/dtos/shift-schedule.dto';

export interface GridCell {
  code: string | null;
  note: string | null;
}

export interface GridRow {
  workerId: string;
  name: string;
  /** Titular o Apoyo. */
  roleLabel: string;
  cells: Record<string, GridCell>;
}

export interface GridCellRef {
  workerId: string;
  date: string;
}

/** Barrido de una fila tras guardar un rango o una rotación: muestra qué días acaban de cambiar. */
export interface GridSweep {
  workerIds: ReadonlySet<string>;
  from: string;
  to: string;
}

interface DayColumn {
  date: string;
  day: number;
  weekday: string;
  sunday: boolean;
  past: boolean;
  today: boolean;
  label: string;
  coverage: DailyCoverage | null;
  missing: string[];
  /** Letras de lo que falta: D (titular de día), N (titular de noche), A (apoyo). */
  missingShort: string;
}

/** Tarjeta flotante que explica la celda señalada, colocada sobre la malla. */
export interface GridTip {
  title: string;
  body: string;
  left: number;
  top: number;
  below: boolean;
}

interface RowGroup {
  label: string;
  rows: { row: GridRow; index: number }[];
}

/**
 * Malla mensual: filas = vendedores, columnas = días. Cabecera y primera columna fijas, flechas para moverse,
 * Enter o Espacio abren el día. En pantallas angostas se muestra un día por tarjeta con deslizamiento.
 */
@Component({
  selector: 'app-schedule-grid',
  standalone: true,
  imports: [ShiftChipComponent, AvatarComponent],
  templateUrl: './schedule-grid.component.html',
  styleUrl: './schedule-grid.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.is-filtering]': '!!filter()' }
})
export class ScheduleGridComponent {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly rows = input.required<GridRow[]>();
  readonly days = input.required<string[]>();
  readonly coverage = input<Record<string, DailyCoverage>>({});
  /** Desde esta fecha se puede editar (igual que el backend: hoy en la zona de la estación). */
  readonly editableFrom = input.required<string>();
  /** Día operativo en curso, resaltado. */
  readonly highlight = input<string>('');
  readonly sweep = input<GridSweep | null>(null);
  /** Celda que acaba de recibir un código: hace un "pop" corto. */
  readonly updatedKey = input<string | null>(null);
  readonly savingKey = input<string | null>(null);
  /** Familia de turno elegida en la leyenda: el resto de la malla se atenúa. */
  readonly filter = input<ShiftTone | null>(null);
  /** Nombre y horario de cada código, para la tarjeta que sale al pasar por una celda. */
  readonly codeInfo = input<Record<string, { name: string; hours: string }>>({});

  @Output() readonly cellOpen = new EventEmitter<GridCellRef>();

  readonly focus = signal({ r: 0, c: 0 });
  /** Tarjeta flotante con el turno de la celda señalada. */
  readonly tip = signal<GridTip | null>(null);

  readonly columns = computed<DayColumn[]>(() => {
    const coverage = this.coverage();
    const from = this.editableFrom();
    const highlight = this.highlight();
    return this.days().map(date => {
      const cov = coverage[date] ?? null;
      return {
        date,
        day: dayOfMonth(date),
        weekday: weekdayShort(date),
        sunday: isSunday(date),
        past: date < from,
        today: date === highlight,
        label: longDateLabel(date),
        coverage: cov,
        missing: cov ? this.missingOf(cov) : [],
        missingShort: cov ? this.missingShortOf(cov) : ''
      };
    });
  });

  /** Titulares y apoyos por separado; el índice global se conserva para moverse con las flechas. */
  readonly groups = computed<RowGroup[]>(() => {
    const groups: RowGroup[] = [];
    this.rows().forEach((row, index) => {
      const label = row.roleLabel === 'Apoyo' ? 'Apoyo' : 'Titulares';
      let group = groups.find(g => g.label === label);
      if (!group) {
        group = { label, rows: [] };
        groups.push(group);
      }
      group.rows.push({ row, index });
    });
    return groups;
  });

  private scrolledFor = '';
  /** Columna a la que hay que llevar la malla en cuanto se vea (si estaba oculta, como en celular). */
  private pendingScroll: number | null = null;

  constructor() {
    // Al cambiar de mes, la malla se desplaza sola hasta el día de hoy (o al inicio si hoy no está en el mes)
    effect(() => {
      const days = this.days();
      const highlight = this.highlight();
      const key = `${days[0] ?? ''}|${highlight}`;
      if (!days.length || key === this.scrolledFor) return;
      this.scrolledFor = key;
      const index = Math.max(0, days.indexOf(highlight));
      this.focus.set({ r: 0, c: index });
      this.pendingScroll = index;
      requestAnimationFrame(() => this.flushScroll());
    });

    const destroyRef = inject(DestroyRef);
    afterNextRender(() => {
      if (typeof ResizeObserver === 'undefined') return;
      const observer = new ResizeObserver(() => this.flushScroll());
      this.host.nativeElement.querySelectorAll('.sg-scroll, .sg-days').forEach(el => observer.observe(el));
      destroyRef.onDestroy(() => observer.disconnect());
    });
  }

  /** Lleva la malla hasta un día (botón "Hoy" o "Ver el primero" sin cobertura). */
  scrollToDate(date: string): void {
    const index = this.days().indexOf(date);
    if (index < 0) return;
    this.pendingScroll = index;
    this.flushScroll();
  }

  private flushScroll(): void {
    if (this.pendingScroll === null) return;
    if (this.scrollToColumn(this.pendingScroll)) this.pendingScroll = null;
  }

  /** El nombre completo va en el title y en la etiqueta accesible; en la columna se usa el corto. */
  shortNameOf(name: string): string {
    return shortName(name);
  }

  key(workerId: string, date: string): string {
    return `${workerId}|${date}`;
  }

  cellOf(row: GridRow, date: string): GridCell {
    return row.cells[date] ?? { code: null, note: null };
  }

  /** Explica la celda señalada: turno, horario, de quién es y la nota si la tiene. */
  showTip(row: GridRow, col: DayColumn, event: Event): void {
    const cell = this.cellOf(row, col.date);
    const info = cell.code ? this.codeInfo()[cell.code] : null;
    const day = col.label.split(' de ')[0];
    const parts = [`${row.name}, ${day}.`];
    if (cell.note) parts.push(cell.note);
    else if (col.past) parts.push('Día pasado: solo lectura.');
    else if (!cell.code) parts.push('Toca para asignar un turno.');

    const hours = info && info.hours && info.hours !== '—' ? ` · ${info.hours}` : '';
    const title = cell.code ? `${info?.name ?? cell.code}${hours}` : 'Sin turno';

    const target = event.currentTarget as HTMLElement;
    const cellBox = target.getBoundingClientRect();
    const hostBox = this.host.nativeElement.getBoundingClientRect();
    const below = cellBox.top - hostBox.top < 110;
    this.tip.set({
      title,
      body: parts.join(' '),
      left: cellBox.left - hostBox.left + cellBox.width / 2,
      top: below ? cellBox.bottom - hostBox.top + 8 : cellBox.top - hostBox.top - 8,
      below
    });
  }

  hideTip(): void {
    this.tip.set(null);
  }

  /** Celda que coincide con el filtro de la leyenda. */
  matched(cell: GridCell): boolean {
    const filter = this.filter();
    return !!filter && shiftTone(cell.code) === filter;
  }

  /** Celda que el filtro deja en segundo plano. */
  dimmed(cell: GridCell): boolean {
    const filter = this.filter();
    return !!filter && shiftTone(cell.code) !== filter;
  }

  isSweeping(row: GridRow, date: string): boolean {
    const sweep = this.sweep();
    return !!sweep && sweep.workerIds.has(row.workerId) && date >= sweep.from && date <= sweep.to;
  }

  cellLabel(row: GridRow, col: DayColumn): string {
    const cell = this.cellOf(row, col.date);
    const parts = [`${row.name}, ${col.label}`, cell.code ? `turno ${cell.code}` : 'sin turno asignado'];
    if (cell.note) parts.push(`nota: ${cell.note}`);
    if (col.past) parts.push('ya pasó, solo lectura');
    return parts.join('. ');
  }

  coverageLabel(col: DayColumn): string {
    if (!col.coverage) return `${col.label}: sin datos de cobertura`;
    return col.coverage.covered ? `${col.label}: cobertura completa` : `${col.label}: falta ${col.missing.join(', ')}`;
  }

  shortLabel(date: string): string {
    return shortDateLabel(date);
  }

  open(row: GridRow, col: DayColumn, r: number, c: number): void {
    this.focus.set({ r, c });
    this.cellOpen.emit({ workerId: row.workerId, date: col.date });
  }

  /** Flechas para moverse por la malla, Inicio/Fin dentro de la fila, Enter o Espacio para abrir el día. */
  onKeydown(event: KeyboardEvent): void {
    const rows = this.rows().length;
    const cols = this.days().length;
    if (!rows || !cols) return;
    let { r, c } = this.focus();
    switch (event.key) {
      case 'ArrowRight': c = Math.min(cols - 1, c + 1); break;
      case 'ArrowLeft': c = Math.max(0, c - 1); break;
      case 'ArrowDown': r = Math.min(rows - 1, r + 1); break;
      case 'ArrowUp': r = Math.max(0, r - 1); break;
      case 'Home': c = 0; break;
      case 'End': c = cols - 1; break;
      default: return;
    }
    event.preventDefault();
    this.moveFocus(r, c);
  }

  onFocusCell(r: number, c: number): void {
    const current = this.focus();
    if (current.r !== r || current.c !== c) this.focus.set({ r, c });
  }

  /** Celda en pantalla, para que la ficha elegida en el panel "vuele" hasta ella. */
  cellElement(workerId: string, date: string): HTMLElement | null {
    return this.host.nativeElement.querySelector<HTMLElement>(
      `.sg-table [data-key="${CSS.escape(this.key(workerId, date))}"]`
    );
  }

  /** Devuelve el foco a la celda tras cerrar el panel (el panel lo intenta, pero la celda pudo redibujarse). */
  focusCell(workerId: string, date: string): void {
    const r = this.rows().findIndex(row => row.workerId === workerId);
    const c = this.days().indexOf(date);
    if (r >= 0 && c >= 0) this.moveFocus(r, c);
  }

  private moveFocus(r: number, c: number): void {
    this.focus.set({ r, c });
    requestAnimationFrame(() => {
      const el = this.host.nativeElement.querySelector<HTMLElement>(`.sg-table [data-r="${r}"][data-c="${c}"]`);
      el?.focus({ preventScroll: true });
      el?.scrollIntoView({ block: 'nearest', inline: 'nearest', behavior: 'smooth' });
    });
  }

  /** Devuelve true si alguna de las dos vistas (tabla o tarjetas) estaba visible y se pudo desplazar. */
  private scrollToColumn(index: number): boolean {
    let done = false;
    const wrap = this.host.nativeElement.querySelector<HTMLElement>('.sg-scroll');
    const cell = this.host.nativeElement.querySelector<HTMLElement>(`.sg-table thead [data-col="${index}"]`);
    if (wrap && cell && wrap.clientWidth > 0) {
      const sticky = this.host.nativeElement.querySelector<HTMLElement>('.sg-table thead .sg-corner')?.offsetWidth ?? 0;
      wrap.scrollLeft = Math.max(0, cell.offsetLeft - sticky - 2 * cell.offsetWidth);
      done = true;
    }
    const strip = this.host.nativeElement.querySelector<HTMLElement>('.sg-days');
    const card = this.host.nativeElement.querySelector<HTMLElement>(`.sg-days [data-col="${index}"]`);
    if (strip && card && strip.clientWidth > 0) {
      strip.scrollLeft = card.offsetLeft - strip.offsetLeft;
      done = true;
    }
    return done;
  }

  private missingShortOf(cov: DailyCoverage): string {
    return (cov.dayCovered ? '' : 'D') + (cov.nightCovered ? '' : 'N') + (cov.supportCovered ? '' : 'A');
  }

  private missingOf(cov: DailyCoverage): string[] {
    const missing: string[] = [];
    if (!cov.dayCovered) missing.push('titular de día');
    if (!cov.nightCovered) missing.push('titular de noche');
    if (!cov.supportCovered) missing.push('apoyo');
    return missing;
  }
}
