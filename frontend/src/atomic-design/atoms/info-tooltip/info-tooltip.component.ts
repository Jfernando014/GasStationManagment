import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, computed, inject, input, signal } from '@angular/core';

const BUBBLE_WIDTH = 260;
const ARROW_SIZE = 10;
const VIEWPORT_MARGIN = 8;
/** Below this much room under the trigger the bubble opens above it. */
const ROOM_BELOW = 110;

let nextId = 0;

/**
 * Button that explains something in a bubble. By default it shows an "i" icon; projected content
 * replaces the icon (`inline`), e.g. a disabled tab that explains why it is disabled.
 * It opens with hover, focus and click (touch screens) and closes with Escape or a click outside.
 * The bubble is fixed to the viewport so no container with `overflow` can clip it.
 */
@Component({
  selector: 'app-info-tooltip',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './info-tooltip.component.html',
  styleUrl: './info-tooltip.component.css',
  host: {
    '(mouseenter)': 'hovered.set(true); place()',
    '(mouseleave)': 'hovered.set(false)',
    '(focusin)': 'focused.set(true); place()',
    '(focusout)': 'focused.set(false)',
    '(document:click)': 'closeOnOutsideClick($event)',
    '(document:keydown.escape)': 'close()',
    '(window:resize)': 'close()'
  }
})
export class InfoTooltipComponent {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /** Text of the bubble. */
  readonly text = input.required<string>();
  /** Accessible name of the button; with projected content the content already names it. */
  readonly label = input('Más información');
  /** Shows the projected content instead of the "i" icon. */
  readonly inline = input(false);

  readonly id = `info-tooltip-${nextId++}`;
  protected readonly hovered = signal(false);
  protected readonly focused = signal(false);
  private readonly pinned = signal(false);
  protected readonly open = computed(() => this.hovered() || this.focused() || this.pinned());
  protected readonly position = signal({ left: 0, top: 0, arrowLeft: 0, above: false });

  constructor() {
    // The bubble is fixed to the viewport: any scroll, also of an inner container, would leave it behind.
    const onScroll = () => this.close();
    document.addEventListener('scroll', onScroll, true);
    inject(DestroyRef).onDestroy(() => document.removeEventListener('scroll', onScroll, true));
  }

  protected toggle(): void {
    this.pinned.update(pinned => !pinned);
    this.place();
  }

  protected close(): void {
    if (!this.open()) return;
    this.hovered.set(false);
    this.focused.set(false);
    this.pinned.set(false);
  }

  protected closeOnOutsideClick(event: Event): void {
    if (!this.host.nativeElement.contains(event.target as Node)) this.close();
  }

  /** Centers the bubble under (or over) the trigger and keeps it inside the viewport. */
  protected place(): void {
    const trigger = this.host.nativeElement.getBoundingClientRect();
    const center = trigger.left + trigger.width / 2;
    const maxLeft = Math.max(VIEWPORT_MARGIN, window.innerWidth - BUBBLE_WIDTH - VIEWPORT_MARGIN);
    const left = Math.min(Math.max(center - BUBBLE_WIDTH / 2, VIEWPORT_MARGIN), maxLeft);
    const above = window.innerHeight - trigger.bottom < ROOM_BELOW;
    this.position.set({
      left,
      top: above ? trigger.top - ARROW_SIZE : trigger.bottom + ARROW_SIZE,
      arrowLeft: Math.min(Math.max(center - left - ARROW_SIZE / 2, 14), BUBBLE_WIDTH - 14 - ARROW_SIZE),
      above
    });
  }
}
