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
          {
            path: 'personal',
            loadComponent: () => import('../features/shifts/pages/day-staff-page/day-staff-page.component').then(m => m.DayStaffPageComponent)
          },
          {
            path: 'vendedores',
            loadComponent: () => import('../features/sales/pages/vendor-close-step2/vendor-close-step2.component')
              .then(m => m.VendorCloseStep2Component)
          },
          {
            path: '',
            pathMatch: 'full',
            redirectTo: 'vendedores'
          }
        ]
      },
      {
        path: 'personal/vendedores',
        loadComponent: () => import('../features/workers/pages/workers-page/workers-page.component').then(m => m.WorkersPageComponent)
      },
      {
        path: 'personal/turnos',
        loadComponent: () => import('../features/shifts/pages/shifts-schedule-page/shifts-schedule-page.component').then(m => m.ShiftsSchedulePageComponent)
      },
      {
        path: 'resultados/metas',
        loadComponent: () => import('../features/annual-goals/pages/annual-goals-page.component').then(m => m.AnnualGoalsPageComponent)
      },
      {
        path: 'resultados/estadisticas',
        loadComponent: () => import('../features/statistics/pages/statistics-page.component').then(m => m.StatisticsPageComponent)
      },
      {
        path: 'resultados/incentivos',
        loadComponent: () => import('../features/incentives/pages/incentives-page/incentives-page.component')
          .then(m => m.IncentivesPageComponent),
        children: [
          {
            path: 'aditivos',
            loadComponent: () => import('../features/incentives/pages/additives-tab/additives-tab.component')
              .then(m => m.AdditivesTabComponent)
          },
          { path: '', pathMatch: 'full', redirectTo: 'aditivos' }
        ]
      },
      {

        path: 'configuracion/tanques',
        loadComponent: () => import('../features/configuration/pages/configuration-page/configuration-page.component').then(m => m.ConfigurationPageComponent)
      },
      {
        path: 'configuracion/aforo',
        loadComponent: () => import('../features/configuration/pages/configuration-page/configuration-page.component').then(m => m.ConfigurationPageComponent)
      },
      {
        path: 'gastos',
        loadComponent: () => import('../atomic-design/pages/administrative-expenses/administrative-expenses-page.component')
          .then(m => m.AdministrativeExpensesPageComponent)
      },
      {
        path: '',
        loadComponent: () => import('../atomic-design/pages/home/home-page.component').then(m => m.HomePageComponent)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
