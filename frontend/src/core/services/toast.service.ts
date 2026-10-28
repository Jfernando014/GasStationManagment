import { Injectable, signal } from '@angular/core';

/** How long a toast stays before it fades away. */
const TOAST_DURATION_MS = 4000;

/** One success message at a time; `<app-toast>` shows it. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private timer: ReturnType<typeof setTimeout> | null = null;

  /** Kept after the toast hides so the text does not vanish during the fade. */
  readonly message = signal('');
  readonly visible = signal(false);

  show(message: string): void {
    if (this.timer !== null) clearTimeout(this.timer);
    this.message.set(message);
    this.visible.set(true);
    this.timer = setTimeout(() => this.hide(), TOAST_DURATION_MS);
  }

  hide(): void {
    if (this.timer !== null) clearTimeout(this.timer);
    this.timer = null;
    this.visible.set(false);
  }
}
