import {
  ChangeDetectionStrategy, Component, DestroyRef, ElementRef, Injector, afterNextRender, inject, input, signal,
  viewChild
} from '@angular/core';

import { ActionButtonDirective } from '../../atoms/action-button/action-button.directive';

/** From this width down the panel is a bottom sheet; it matches the media query of the styles. */
const SHEET_QUERY = '(max-width: 639.98px)';
const VIEWPORT_MARGIN = 12;
const GAP = 8;
const FOCUSABLE = 'a[href], button:not([disabled]), input, select, textarea, [tabindex]:not([tabindex="-1"])';

let nextId = 0;

/**
 * Button that opens a dialog next to it with the projected content; on a phone the dialog is a bottom sheet with a
 * close button. It closes with Escape, with a click outside and when the focus leaves it, and gives the focus back
 * to its button. `[popoverIcon]` projects the icon of the button. The panel is fixed to the viewport so no container
 * can clip it.
 */
@Component({
  selector: 'app-popover',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ActionButtonDirective],
  templateUrl: './popover.component.html',
  styleUrl: './popover.component.css',
  host: {
    '(document:click)': 'closeOnOutsideClick($event)',
    '(document:keydown.escape)': 'close(true)',
    '(window:resize)': 'place()',
    '(focusout)': 'closeOnFocusLeave($event)'
  }
})
export class PopoverComponent {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly trigger = viewChild.required<ElementRef<HTMLButtonElement>>('trigger');
  private readonly panel = viewChild.required<ElementRef<HTMLElement>>('panel');

  /** Text of the button and title of the dialog. */
  readonly label = input.required<string>();
  /** Width of the dialog next to the button, in px. */
  readonly width = input(660);

  readonly id = `popover-${nextId++}`;
  readonly open = signal(false);
  /** True from the first opening on: the content can wait until then to be created. */
  readonly opened = signal(false);
  protected readonly sheet = signal(false);
  protected readonly position = signal({ top: 0, left: 0, maxHeight: 0 });

  constructor() {
    // The panel is fixed: when a container scrolls it has to follow its button.
    const onScroll = () => this.place();
    document.addEventListener('scroll', onScroll, true);
    inject(DestroyRef).onDestroy(() => document.removeEventListener('scroll', onScroll, true));
  }

  protected toggle(): void {
    if (this.open()) {
      this.close(true);
      return;
    }
    this.open.set(true);
    this.opened.set(true);
    this.place();
    afterNextRender(() => this.panel().nativeElement.focus({ preventScroll: true }), { injector: this.injector });
  }

  close(restoreFocus = false): void {
    if (!this.open()) return;
    this.open.set(false);
    if (restoreFocus) this.trigger().nativeElement.focus({ preventScroll: true });
  }

  protected closeOnOutsideClick(event: Event): void {
    if (!this.host.nativeElement.contains(event.target as Node)) this.close();
  }

  protected closeOnFocusLeave(event: FocusEvent): void {
    const next = event.relatedTarget as Node | null;
    if (!this.sheet() && next && !this.host.nativeElement.contains(next)) this.close();
  }

  /** The bottom sheet covers the page, so Tab stays inside it. */
  protected keepFocus(event: Event): void {
    if (!this.sheet()) return;
    const { key, shiftKey } = event as KeyboardEvent;
    if (key !== 'Tab') return;
    const panel = this.panel().nativeElement;
    const items = Array.from(panel.querySelectorAll<HTMLElement>(FOCUSABLE));
    const first = items[0] ?? panel;
    const last = items[items.length - 1] ?? panel;
    const edge = shiftKey ? first : last;
    if (document.activeElement === edge || document.activeElement === panel) {
      event.preventDefault();
      (shiftKey ? last : first).focus();
    }
  }

  /** Under the button, aligned to its right edge and inside the viewport. */
  protected place(): void {
    if (!this.open()) return;
    this.sheet.set(window.matchMedia?.(SHEET_QUERY).matches ?? false);
    const button = this.trigger().nativeElement.getBoundingClientRect();
    const width = Math.min(this.width(), window.innerWidth - VIEWPORT_MARGIN * 2);
    const left = Math.min(
      Math.max(button.right - width, VIEWPORT_MARGIN), window.innerWidth - width - VIEWPORT_MARGIN);
    const top = button.bottom + GAP;
    this.position.set({ top, left, maxHeight: Math.max(220, window.innerHeight - top - VIEWPORT_MARGIN) });
  }
}
