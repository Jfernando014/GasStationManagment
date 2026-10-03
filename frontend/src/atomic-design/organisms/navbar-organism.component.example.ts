import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Example Atomic Design - Organism: Navbar Component
 */
@Component({
  selector: 'app-navbar-organism-example',
  standalone: true,
  imports: [CommonModule],
  template: 
    <nav class="navbar">
      <h1>Gas Station Management</h1>
    </nav>
  
})
export class NavbarOrganismExampleComponent {
  appTitle = 'Gas Station App';
}
