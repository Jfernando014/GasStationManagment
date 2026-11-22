import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ActionButtonDirective } from '../../atoms/action-button/action-button.directive';

/** Blurred modal asking for confirmation before a destructive action. */
@Component({
  selector: 'app-confirm-modal',
  standalone: true,
  imports: [CommonModule, ActionButtonDirective],
  templateUrl: './confirm-modal.component.html',
  styleUrl: './confirm-modal.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown.escape)': 'onEscapeKey()'
  }
})
export class ConfirmModalComponent {
  readonly open = input(false);
  readonly title = input('Confirmar eliminación');
  readonly subtitle = input('Esta acción no se puede deshacer');
  readonly message = input('');
  readonly confirmLabel = input('Eliminar');
  readonly busy = input(false);

  readonly confirmed = output<void>();
  readonly cancelled = output<void>();

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget && !this.busy()) {
      this.cancelled.emit();
    }
  }

  onEscapeKey(): void {
    if (this.open() && !this.busy()) {
      this.cancelled.emit();
    }
  }
}
