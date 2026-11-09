import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  effect,
  inject,
  input,
  output,
  signal
} from '@angular/core';

export interface TabItem {
  id: string;
  label: string;
}

/**
 * Accessible tabs with a sliding underline. It renders only the tab list: the page draws each panel with
 * `role="tabpanel"`, `id="<idPrefix>-panel-<id>"` and `aria-labelledby="<idPrefix>-tab-<id>"`.
 * Left and right arrows (and Home / End) move between tabs and select them.
 */
@Component({
  selector: 'app-tabs',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './tabs.component.html',
  styleUrl: './tabs.component.css',
  host: { '(window:resize)': 'measure()' }
})
export class TabsComponent {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly destroyRef = inject(DestroyRef);

  readonly tabs = input.required<TabItem[]>();
  readonly active = input.required<string>();
  /** Accessible name of the tab list. */
  readonly label = input('');
  /** Prefix of the ids of the tabs and panels, so several tab lists can live on one page. */
  readonly idPrefix = input('tabs');
  readonly activeChange = output<string>();

  protected readonly indicator = signal({ left: 0, width: 0 });
  /** The underline jumps to the first tab without sliding; it only slides after that. */
  protected readonly ready = signal(false);

  constructor() {
    effect(() => {
      this.active();
      this.tabs();
      requestAnimationFrame(() => this.measure());
    });
    afterNextRender(() => {
      if (typeof ResizeObserver === 'undefined') return;
      const observer = new ResizeObserver(() => this.measure());
      observer.observe(this.host.nativeElement);
      this.destroyRef.onDestroy(() => observer.disconnect());
    });
  }

  protected tabId(id: string): string {
    return `${this.idPrefix()}-tab-${id}`;
  }

  protected panelId(id: string): string {
    return `${this.idPrefix()}-panel-${id}`;
  }

  protected select(id: string): void {
    if (id !== this.active()) {
      this.activeChange.emit(id);
    }
  }

  protected onKeydown(event: KeyboardEvent): void {
    const tabs = this.tabs();
    const current = tabs.findIndex(tab => tab.id === this.active());
    let next: number;
    switch (event.key) {
      case 'ArrowRight':
        next = (current + 1) % tabs.length;
        break;
      case 'ArrowLeft':
        next = (current - 1 + tabs.length) % tabs.length;
        break;
      case 'Home':
        next = 0;
        break;
      case 'End':
        next = tabs.length - 1;
        break;
      default:
        return;
    }
    event.preventDefault();
    const target = tabs[next];
    this.host.nativeElement.querySelector<HTMLElement>(`#${this.tabId(target.id)}`)?.focus();
    this.select(target.id);
  }

  protected measure(): void {
    const button = this.host.nativeElement.querySelector<HTMLElement>('[role="tab"][aria-selected="true"]');
    if (!button) return;
    this.indicator.set({ left: button.offsetLeft, width: button.offsetWidth });
    if (!this.ready()) {
      requestAnimationFrame(() => this.ready.set(true));
    }
  }
}
