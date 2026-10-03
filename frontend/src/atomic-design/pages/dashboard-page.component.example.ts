import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavbarOrganismExampleComponent } from '../organisms/navbar-organism.component.example';

/**
 * Example Atomic Design - Page: Dashboard Page Component
 */
@Component({
  selector: 'app-dashboard-page-example',
  standalone: true,
  imports: [CommonModule, NavbarOrganismExampleComponent],
  template: 
    <app-navbar-organism-example></app-navbar-organism-example>
    <main class="dashboard-page">
      <h2>Dashboard Overview</h2>
    </main>
  
})
export class DashboardPageExampleComponent {
  pageTitle = 'Dashboard';
}
