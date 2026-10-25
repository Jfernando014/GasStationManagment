import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './dashboard-layout.component.html',
  styleUrl: './dashboard-layout.component.css',
  host: { '(document:keydown.escape)': 'closeMenu()', '[class.statistics-mobile-layout]': 'statisticsActive()' }
})
export class DashboardLayoutComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  readonly configurationOpen = signal(true);
  readonly resultsOpen = signal(true);
  readonly mobileMenuOpen = signal(false);
  readonly statisticsActive = signal(this.router.url.split(/[?#]/)[0] === '/resultados/estadisticas');
  private readonly sidebar = viewChild<ElementRef<HTMLElement>>('sidebar');
  private readonly menuButton = viewChild<ElementRef<HTMLButtonElement>>('menuButton');

  constructor() {
    this.router.events.pipe(filter(event => event instanceof NavigationEnd), takeUntilDestroyed())
      .subscribe(event => {
        this.statisticsActive.set(event.urlAfterRedirects.split(/[?#]/)[0] === '/resultados/estadisticas');
        this.closeMenu(false);
      });
  }

  toggleMenu(): void {
    if (this.mobileMenuOpen()) { this.closeMenu(); return; }
    this.mobileMenuOpen.set(true);
    queueMicrotask(() => this.sidebar()?.nativeElement.querySelector<HTMLElement>('a[aria-current="page"], button')?.focus());
  }

  closeMenu(restoreFocus = true): void {
    if (!this.mobileMenuOpen()) return;
    this.mobileMenuOpen.set(false);
    if (restoreFocus) this.menuButton()?.nativeElement.focus();
  }

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
