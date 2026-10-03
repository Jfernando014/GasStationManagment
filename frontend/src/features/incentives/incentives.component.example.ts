import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Example Feature Component: Incentives
 */
@Component({
  selector: 'app-incentives-example',
  standalone: true,
  imports: [CommonModule],
  template: 
    <section class="incentives-container">
      <h3>Incentives Feature</h3>
      <p>Placeholder component for incentives feature module.</p>
    </section>
  
})
export class IncentivesComponentExample {
  featureName = 'incentives';
}
