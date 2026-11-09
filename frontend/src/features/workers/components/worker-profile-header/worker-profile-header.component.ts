import { ChangeDetectionStrategy, Component, ElementRef, input, output, viewChild } from '@angular/core';

import { AvatarComponent } from '../../../../atomic-design/atoms/avatar/avatar.component';
import { StatusBadgeComponent } from '../../../../atomic-design/atoms/status-badge/status-badge.component';
import { toNameCase } from '../../../../shared/helpers/text.helpers';
import { Worker } from '../../../../shared/models/dtos/worker.dto';

/** Top card of the profile: who the worker is, with the button to edit the data. */
@Component({
  selector: 'app-worker-profile-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AvatarComponent, StatusBadgeComponent],
  templateUrl: './worker-profile-header.component.html',
  styleUrl: './worker-profile-header.component.css'
})
export class WorkerProfileHeaderComponent {
  private readonly editButton = viewChild.required<ElementRef<HTMLButtonElement>>('editButton');

  readonly worker = input.required<Worker>();
  readonly editRequested = output<void>();

  protected roleLabel(worker: Worker): string {
    return toNameCase(worker.roleName);
  }

  /** Gives the focus back to the edit button, for example after closing the edit dialog. */
  focusEdit(): void {
    this.editButton().nativeElement.focus();
  }
}
