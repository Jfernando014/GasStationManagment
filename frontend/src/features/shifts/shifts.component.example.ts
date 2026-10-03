import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Example Feature Component: Shifts
 */
@Component({
  selector: 'app-shifts-example',
  standalone: true,
  imports: [CommonModule],
  template: 
    <section class="shifts-container">
      <h3>Shifts Feature</h3>
      <p>Placeholder component for shifts feature module.</p>
    </section>
  
})
export class ShiftsComponentExample {
  featureName = 'shifts';
}
