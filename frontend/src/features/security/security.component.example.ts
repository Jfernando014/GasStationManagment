import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Example Feature Component: Security
 */
@Component({
  selector: 'app-security-example',
  standalone: true,
  imports: [CommonModule],
  template: 
    <section class="security-container">
      <h3>Security Feature</h3>
      <p>Placeholder component for security feature module.</p>
    </section>
  
})
export class SecurityComponentExample {
  featureName = 'security';
}
