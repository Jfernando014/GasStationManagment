import { Component, computed, input } from '@angular/core';

/** Circle with the initials of a name (first letter of the first two words). */
@Component({
  selector: 'app-avatar',
  standalone: true,
  template: '{{ initials() }}',
  host: {
    class: 'ps-avatar',
    '[class.ps-avatar--lg]': "size() === 'lg'",
    'aria-hidden': 'true'
  }
})
export class AvatarComponent {
  readonly name = input.required<string>();
  readonly size = input<'md' | 'lg'>('md');

  readonly initials = computed(() =>
    this.name()
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map(word => word.charAt(0).toUpperCase())
      .join('')
  );
}
