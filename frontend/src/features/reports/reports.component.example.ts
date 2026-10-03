import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Example Feature Component: Reports
 */
@Component({
  selector: 'app-reports-example',
  standalone: true,
  imports: [CommonModule],
  template: 
    <section class="reports-container">
      <h3>Reports Feature</h3>
      <p>Placeholder component for reports feature module.</p>
    </section>
  
})
export class ReportsComponentExample {
  featureName = 'reports';
}
