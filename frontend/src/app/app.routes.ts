import { Routes } from '@angular/router';
import { LoginPageComponent } from '../atomic-design/pages/login/login.component';
import { authGuard } from '../core/guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginPageComponent },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      // Componentes protegidos
    ]
  },
  { path: '**', redirectTo: '' }
];
