import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Example Atomic Design - Atom: Button Component
 * Follows lowerCamelCase for inputs/outputs and PascalCase for component.
 */
@Component({
  selector: 'app-button-atom-example',
  standalone: true,
  imports: [CommonModule],
  template: 
    <button [type]="type" [disabled]="disabled" (click)="handleClick($event)">
      <ng-content></ng-content>
    </button>
  
})
export class ButtonAtomExampleComponent {
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
  @Input() disabled = false;
  @Output() buttonClick = new EventEmitter<MouseEvent>();

  handleClick(event: MouseEvent): void {
    if (!this.disabled) {
      this.buttonClick.emit(event);
    }
  }
}
