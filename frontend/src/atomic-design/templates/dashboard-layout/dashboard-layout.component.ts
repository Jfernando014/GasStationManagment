import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './dashboard-layout.component.html'
})
export class DashboardLayoutComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  readonly configurationOpen = signal(true);
  readonly resultsOpen = signal(true);

  toggleConfiguration(): void {
    this.configurationOpen.update(open => !open);
  }

  toggleResults(): void {
    this.resultsOpen.update(open => !open);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
