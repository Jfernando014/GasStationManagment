import { Component, computed, inject, input } from '@angular/core';

import { ColorThemeService } from '../../../core/services/color-theme.service';
import { normalizeHexColor, readableTextOn } from '../../../shared/helpers/color-contrast.helpers';

/** Circle with the initials of a name (first letter of the first two words), tinted with the seller's custom color when configured. */
@Component({
  selector: 'app-avatar',
  standalone: true,
  template: '{{ initials() }}',
  host: {
    class: 'ps-avatar',
    '[class.ps-avatar--lg]': "size() === 'lg'",
    '[style.background]': 'resolvedStyle()?.background ?? null',
    '[style.color]': 'resolvedStyle()?.text ?? null',
    'aria-hidden': 'true'
  }
})
export class AvatarComponent {
  private readonly colorTheme = inject(ColorThemeService, { optional: true });

  readonly name = input.required<string>();
  readonly size = input<'md' | 'lg'>('md');
  /** Optional worker UUID for exact seller color lookup. */
  readonly workerId = input<string | null>(null);
  /** Optional worker document (cédula) for views that identify sellers by document. */
  readonly document = input<string | null>(null);
  /** Optional explicit hex override (used for live previews inside the color settings screen). */
  readonly color = input<string | null>(null);

  readonly initials = computed(() =>
    this.name()
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map(word => word.charAt(0).toUpperCase())
      .join('')
  );

  readonly resolvedStyle = computed<{ background: string; text: string } | null>(() => {
    const explicitHex = normalizeHexColor(this.color());
    if (explicitHex) {
      return { background: explicitHex, text: readableTextOn(explicitHex) };
    }
    const resolved = this.colorTheme?.resolveWorkerColor({
      workerId: this.workerId(),
      document: this.document(),
      name: this.name()
    });
    return resolved ? { background: resolved.background, text: resolved.text } : null;
  });
}
