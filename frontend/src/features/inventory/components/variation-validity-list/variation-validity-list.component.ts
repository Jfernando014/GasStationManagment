import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ActionButtonDirective } from '../../../../atomic-design/atoms/action-button/action-button.directive';
import { TagComponent } from '../../../../atomic-design/atoms/tag/tag.component';

/** One validity date of the list, with its texts ready to show. */
export interface VariationValidityItem {
  id: number;
  /** "dd/mm/aaaa" */
  date: string;
  /** It rules today. */
  current: boolean;
  /** The oldest one, which covers every earlier month. */
  base: boolean;
  months: string;
  summary: string;
}

/**
 * Validity dates of the alert settings, the latest first, with the explanation of how a month picks one of them.
 * On a phone it fills the screen and the button for a new one goes under the cards.
 */
@Component({
  selector: 'app-variation-validity-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ActionButtonDirective, TagComponent],
  templateUrl: './variation-validity-list.component.html',
  styleUrl: './variation-validity-list.component.css'
})
export class VariationValidityListComponent {
  readonly items = input.required<VariationValidityItem[]>();
  readonly selectedId = input<number | null>(null);
  readonly disabled = input(false);
  readonly chosen = output<number>();
  readonly create = output<void>();

  /** The little calendar only illustrates a month: it does not depend on the data. */
  protected readonly days = Array.from({ length: 30 }, (_, index) => index + 1);
  protected readonly exampleDay = 15;
}
