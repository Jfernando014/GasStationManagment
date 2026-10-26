import { Component, ElementRef, EventEmitter, Output, ViewChild, effect, input } from '@angular/core';

/**
 * Panel lateral no intrusivo (no bloquea el resto de la pantalla como un modal centrado): se desliza desde
 * la derecha, cierra con Escape o clic en el fondo, y devuelve el foco al elemento que lo abrió.
 * Reutilizado por la edición de un día de la malla, el asistente de rotación y el registro de novedades.
 */
@Component({
  selector: 'app-side-panel',
  standalone: true,
  templateUrl: './side-panel.component.html',
  styleUrl: './side-panel.component.css',
  host: { '(document:keydown.escape)': 'onEscape()' }
})
export class SidePanelComponent {
  readonly open = input(false);
  readonly heading = input.required<string>();
  readonly description = input('');
  /** Ancho del panel en px; el asistente de rotación necesita más espacio que un formulario simple. */
  readonly width = input(420);

  @Output() readonly closed = new EventEmitter<void>();
  @ViewChild('panel') private readonly panelRef?: ElementRef<HTMLElement>;
  private lastFocused: HTMLElement | null = null;

  constructor() {
    effect(() => {
      if (this.open()) {
        this.lastFocused = document.activeElement as HTMLElement;
        queueMicrotask(() => {
          const panel = this.panelRef?.nativeElement;
          panel?.querySelector<HTMLElement>('button, [href], input, select, textarea, [tabindex]')?.focus();
        });
      } else {
        this.lastFocused?.focus();
        this.lastFocused = null;
      }
    });
  }

  onEscape(): void {
    if (this.open()) {
      this.close();
    }
  }

  onBackdropClick(): void {
    this.close();
  }

  close(): void {
    this.closed.emit();
  }
}
