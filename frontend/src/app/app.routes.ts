import { Routes } from '@angular/router';
import { guestGuard } from './core/guards/guest.guard';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/register/register.component').then(
        (m) => m.RegisterComponent,
      ),
    title: 'Sign Up – Pcenter',
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/login/login.component').then(
        (m) => m.LoginComponent,
      ),
    title: 'Log In – Pcenter',
  },
  {
    path: 'rooms',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/rooms/rooms-view.component').then(
        (m) => m.RoomsViewComponent,
      ),
    title: 'Rooms – Pcenter',
  },
  {
    path: 'rooms/create',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/rooms/create-room/create-room.component').then(
        (m) => m.CreateRoomComponent,
      ),
    title: 'Create Room – Pcenter',
  },
  {
    path: 'rooms/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/rooms/room-detail/room-detail.component').then(
        (m) => m.RoomDetailComponent,
      ),
    title: 'Room Details – Pcenter',
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/profile/dashboard/profile-dashboard.component').then(
        (m) => m.ProfileDashboardComponent,
      ),
    title: 'Profile – Pcenter',
  },
  {
    path: 'analytics',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/analytics/analytics-dashboard/analytics-dashboard.component').then(
        (m) => m.AnalyticsDashboardComponent,
      ),
    title: 'Analytics – Pcenter',
  },
  {
    path: '404',
    loadComponent: () =>
      import('./features/not-found/not-found.component').then(
        (m) => m.NotFoundComponent,
      ),
    title: '404 - Page Not Found – Pcenter',
  },
  {
    path: '',
    redirectTo: '/register',
    pathMatch: 'full',
  },
  {
    // Catch-all route to redirect any non-existent route to 404 page
    path: '**',
    redirectTo: '/404',
  }
];


