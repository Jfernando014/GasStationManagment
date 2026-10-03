import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Example Feature Component: Workers
 */
@Component({
  selector: 'app-workers-example',
  standalone: true,
  imports: [CommonModule],
  template: 
    <section class="workers-container">
      <h3>Workers Feature</h3>
      <p>Placeholder component for workers feature module.</p>
    </section>
  
})
export class WorkersComponentExample {
  featureName = 'workers';
}
