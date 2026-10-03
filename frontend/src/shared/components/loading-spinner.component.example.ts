import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Example Shared Component: Loading Spinner
 */
@Component({
  selector: 'app-loading-spinner-example',
  standalone: true,
  imports: [CommonModule],
  template: 
    <div class="spinner-container" *ngIf="isLoading">
      <div class="spinner"></div>
      <p>{{ loadingMessage }}</p>
    </div>
  
})
export class LoadingSpinnerComponentExample {
  @Input() isLoading = false;
  @Input() loadingMessage = 'Loading...';
}
