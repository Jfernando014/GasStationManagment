import { afterNextRender, Component, ElementRef, input, output, viewChild } from '@angular/core';

@Component({ selector: 'app-statistics-dialog', standalone: true,
  template: `
    <dialog #dialog class="stats-dialog" tabindex="-1" aria-labelledby="statistics-dialog-title" (cancel)="cancel($event)" (click)="backdrop($event)" (keydown.escape)="cancel($event)">
      <div class="dialog-header">@if (showBack()) { <button type="button" class="jr-btn jr-btn--sec jr-btn--sm" (click)="closed.emit()">← Volver al reporte</button> }
        <div class="dialog-titles"><h2 id="statistics-dialog-title">{{ heading() }}</h2><p>{{ subtitle() }}</p></div>
        <ng-content select="[dialogControls]" />
        <button type="button" class="btn-dialog-close" aria-label="Cerrar diálogo" (click)="closed.emit()">✕</button>
      </div>
      <div class="dialog-body"><ng-content /></div>
    </dialog>
  `,
  styles: `
    .stats-dialog { padding: 0; border: 1px solid var(--border-main); border-radius: var(--radius-floating); width: min(980px, 94vw); max-width: 94vw; max-height: 90dvh; margin: auto; background: var(--surface-card); color: var(--text-body); box-shadow: var(--shadow-modal); }
    .stats-dialog::backdrop { background: rgba(11,25,44,.46); backdrop-filter: blur(5px); }
    .dialog-header { display: flex; gap: 12px; align-items: center; padding: 20px 24px; flex-wrap: wrap; }
    .dialog-titles { flex: 1; min-width: 150px; }
    h2 { font-size: 16px; color: var(--text-heading); margin: 0; }
    .dialog-titles p { font-size: 12px; margin-top: 4px; color: var(--text-muted); }
    .dialog-body { padding: 24px; overflow: auto; }
    @media(max-width:600px) { .dialog-header,.dialog-body { padding: 16px; } }
  `
})
export class StatisticsDialogComponent {
  readonly heading = input.required<string>();
  readonly subtitle = input('');
  readonly showBack = input(false);
  readonly closed = output<void>();
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  constructor() {
    afterNextRender(() => {
      const dialog = this.dialog().nativeElement;
      if (typeof dialog.showModal === 'function') dialog.showModal(); else dialog.setAttribute('open', '');
    });
  }
  cancel(event: Event): void { event.preventDefault(); this.closed.emit(); }
  backdrop(event: MouseEvent): void { if (event.target === this.dialog().nativeElement) this.closed.emit(); }
}
