import { Routes } from '@angular/router';
import { LoginPageComponent } from '../atomic-design/pages/login/login.component';
import { DashboardLayoutComponent } from '../atomic-design/templates/dashboard-layout/dashboard-layout.component';
import { authGuard } from '../core/guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginPageComponent },
  {
    path: '',
    component: DashboardLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'jornada',
        loadComponent: () => import('../atomic-design/pages/journal/journal-page.component').then(m => m.JournalPageComponent),
        children: [
          // Pantallas de cada paso como rutas hijas (a ser implementadas por los dueños)
          // { path: 'step-1-medicion-inicial', component: ... }
          // { path: 'step-2-asignacion-turnos', component: ... }
        ]
      },
      {
        path: '',
        loadComponent: () => import('../atomic-design/pages/home/home-page.component').then(m => m.HomePageComponent)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
