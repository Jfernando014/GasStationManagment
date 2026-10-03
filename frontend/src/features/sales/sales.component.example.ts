import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Example Feature Component: Sales
 */
@Component({
  selector: 'app-sales-example',
  standalone: true,
  imports: [CommonModule],
  template: 
    <section class="sales-container">
      <h3>Sales Feature</h3>
      <p>Placeholder component for sales feature module.</p>
    </section>
  
})
export class SalesComponentExample {
  featureName = 'sales';
}
