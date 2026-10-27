import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, afterNextRender, inject, input, output, viewChild
} from '@angular/core';

/**
 * Small confirmation over the current screen. It is a native modal `<dialog>`: the rest of the page is inert,
 * Tab stays inside, Escape cancels, and the focus goes back to where it was when it closes.
 * Create it with `@if` when it is needed; it opens itself.
 */
@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <dialog #dialog class="confirm" role="alertdialog" aria-labelledby="confirm-title"
      (cancel)="$event.preventDefault(); cancelled.emit()">
      <h2 id="confirm-title">{{ heading() }}</h2>
      @if (message()) {
        <p>{{ message() }}</p>
      }
      <div class="actions">
        <button type="button" class="secondary" autofocus (click)="cancelled.emit()">{{ cancelLabel() }}</button>
        <button type="button" class="danger" (click)="confirmed.emit()">{{ confirmLabel() }}</button>
      </div>
    </dialog>
  `,
  styleUrl: './confirm-dialog.component.css'
})
export class ConfirmDialogComponent {
  readonly heading = input.required<string>();
  readonly message = input('');
  readonly confirmLabel = input('Confirmar');
  readonly cancelLabel = input('Cancelar');

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    // The component is removed from the page without `close()`, so the focus is given back by hand.
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    inject(DestroyRef).onDestroy(() => opener?.focus());
    afterNextRender(() => {
      const dialog = this.dialog().nativeElement;
      if (!dialog.open) dialog.showModal();
    });
  }
}
