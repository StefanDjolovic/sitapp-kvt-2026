import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'conversations',
  },
  {
    path: 'conversations',
    loadComponent: () =>
      import('./features/conversations-list/conversations-list.component').then(
        (component) => component.ConversationsListComponent,
      ),
    title: 'Razgovori | SitApp',
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
    path: 'conversations/:id',
    loadComponent: () =>
      import('./features/conversation/conversation-page.component').then(
        (component) => component.ConversationPageComponent,
      ),
    title: 'Razgovor | SitApp',
  },
  {
    path: '**',
    redirectTo: 'conversations',
  },
];
