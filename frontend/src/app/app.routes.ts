import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'users/search',
  },
  {
    path: 'users/search',
    loadComponent: () =>
      import('./features/user-search/user-search.component').then(
        (component) => component.UserSearchComponent,
      ),
    title: 'Pretraga korisnika | SitApp',
  },
  {
    path: '**',
    redirectTo: 'users/search',
  },
];
