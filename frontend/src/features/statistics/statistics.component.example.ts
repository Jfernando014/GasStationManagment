import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Example Feature Component: Statistics
 */
@Component({
  selector: 'app-statistics-example',
  standalone: true,
  imports: [CommonModule],
  template: 
    <section class="statistics-container">
      <h3>Statistics Feature</h3>
      <p>Placeholder component for statistics feature module.</p>
    </section>
  
})
export class StatisticsComponentExample {
  featureName = 'statistics';
}
