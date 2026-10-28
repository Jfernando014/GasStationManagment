import { Component, DestroyRef, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, RouterLink, RouterLinkActive, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { ChangePasswordModalComponent } from '../../organisms/change-password-modal/change-password-modal.component';

/** Clave del ancho de la barra lateral: se recuerda entre visitas, como el resto de preferencias de la vista. */
const COLLAPSED_KEY = 'eds.sidebar.collapsed';

/** Debajo de este ancho la barra lateral ya es un cajón; contraerla no aplica. */
const NARROW_QUERY = '(max-width: 768px)';

function storedCollapsed(): boolean {
  try {
    return localStorage.getItem(COLLAPSED_KEY) === '1';
  } catch {
    return false;
  }
}

/** Pantallas que ya se adaptan al celular: en ellas la barra lateral pasa a ser un menú desplegable. */
const MOBILE_READY_ROUTES = ['/resultados/estadisticas', '/personal/turnos', '/jornada/personal'];

function usesMobileLayout(url: string): boolean {
  return MOBILE_READY_ROUTES.includes(url.split(/[?#]/)[0]);
}

@Component({
  selector: 'app-dashboard-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ChangePasswordModalComponent],
  templateUrl: './dashboard-layout.component.html',
  styleUrl: './dashboard-layout.component.css',
  host: {
    '(document:keydown.escape)': 'closeMenu()',
    '[class.statistics-mobile-layout]': 'statisticsActive()',
    '[class.sidebar-collapsed]': 'sidebarCollapsed()'
  }
})
export class DashboardLayoutComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
  readonly configurationOpen = signal(true);
  readonly resultsOpen = signal(true);
  readonly personalOpen = signal(true);
  readonly mobileMenuOpen = signal(false);
  readonly changePasswordModalOpen = signal(false);
  readonly statisticsActive = signal(usesMobileLayout(this.router.url));
  /** Preferencia del usuario; en pantallas angostas no se aplica porque ahí la barra es un cajón. */
  private readonly collapsePreference = signal(storedCollapsed());
  private readonly narrow = signal(false);
  readonly sidebarCollapsed = computed(() => this.collapsePreference() && !this.narrow());
  /** Nombre de la ruta señalada mientras la barra está contraída, al lado del icono. */
  readonly navTip = signal<{ text: string; left: number; top: number } | null>(null);
  private readonly sidebar = viewChild<ElementRef<HTMLElement>>('sidebar');
  private readonly menuButton = viewChild<ElementRef<HTMLButtonElement>>('menuButton');

  constructor() {
    const media = typeof window === 'undefined' ? null : window.matchMedia?.(NARROW_QUERY) ?? null;
    if (media) {
      // Se escuchan los dos eventos: algunos navegadores embebidos no emiten el 'change' de la media query.
      const sync = () => this.narrow.set(media.matches);
      sync();
      media.addEventListener('change', sync);
      window.addEventListener('resize', sync);
      inject(DestroyRef).onDestroy(() => {
        media.removeEventListener('change', sync);
        window.removeEventListener('resize', sync);
      });
    }

    this.router.events.pipe(filter(event => event instanceof NavigationEnd), takeUntilDestroyed())
      .subscribe(event => {
        this.statisticsActive.set(usesMobileLayout(event.urlAfterRedirects));
        this.closeMenu(false);
      });
  }

  /** El logo contrae y expande la barra; el ancho que suelta lo toma el contenido de la derecha. */
  toggleSidebar(): void {
    const collapsed = !this.collapsePreference();
    this.collapsePreference.set(collapsed);
    this.navTip.set(null);
    try {
      localStorage.setItem(COLLAPSED_KEY, collapsed ? '1' : '0');
    } catch {
      // Sin almacenamiento la preferencia dura lo que la sesión; no es motivo para fallar.
    }
  }

  /** Con la barra contraída, el nombre de la ruta sale al lado del icono para no perder el contexto. */
  onNavPointer(event: Event): void {
    if (!this.sidebarCollapsed()) {
      this.navTip.set(null);
      return;
    }
    const button = (event.target as HTMLElement | null)?.closest<HTMLElement>('.sidebar-action-btn');
    if (!button) {
      this.navTip.set(null);
      return;
    }
    const text = button.querySelector('.btn-prefix-stack span')?.textContent?.trim() ?? '';
    const box = button.getBoundingClientRect();
    this.navTip.set({ text, left: box.right + 12, top: box.top + box.height / 2 });
  }

  hideNavTip(): void {
    this.navTip.set(null);
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
    if (this.expandBeforeOpening(this.configurationOpen)) return;
    this.configurationOpen.update(open => !open);
  }

  toggleResults(): void {
    if (this.expandBeforeOpening(this.resultsOpen)) return;
    this.resultsOpen.update(open => !open);
  }

  togglePersonal(): void {
    if (this.expandBeforeOpening(this.personalOpen)) return;
    this.personalOpen.update(open => !open);
  }

  /** Con la barra contraída los submenús no caben: el primer clic la expande y abre el grupo. */
  private expandBeforeOpening(group: { set(open: boolean): void }): boolean {
    if (!this.sidebarCollapsed()) return false;
    this.toggleSidebar();
    group.set(true);
    return true;
  }

  openChangePasswordModal(): void {
    this.changePasswordModalOpen.set(true);
    if (this.mobileMenuOpen()) {
      this.closeMenu(false);
    }
  }

  closeChangePasswordModal(): void {
    this.changePasswordModalOpen.set(false);
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
