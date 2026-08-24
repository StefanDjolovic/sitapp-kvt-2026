import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { DEVELOPMENT_USER_ID } from '../../core/development-user';
import { User } from '../../models/user.model';
import { ConversationService } from '../../services/conversation.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-user-search',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './user-search.component.html',
  styleUrl: './user-search.component.scss',
})
export class UserSearchComponent {
  private readonly userService = inject(UserService);
  private readonly conversationService = inject(ConversationService);
  private readonly router = inject(Router);

  query = '';
  users: User[] = [];
  selectedUser: User | null = null;
  loading = false;
  openingUserId: number | null = null;
  hasSearched = false;
  errorMessage = '';
  conversationErrorMessage = '';

  onQueryChange(): void {
    if (this.query.trim()) {
      return;
    }

    this.users = [];
    this.selectedUser = null;
    this.hasSearched = false;
    this.errorMessage = '';
    this.conversationErrorMessage = '';
  }

  search(): void {
    const query = this.query.trim();

    if (!query || this.loading || this.openingUserId !== null) {
      return;
    }

    this.loading = true;
    this.hasSearched = true;
    this.errorMessage = '';
    this.conversationErrorMessage = '';
    this.users = [];
    this.selectedUser = null;

    this.userService
      .search(query, DEVELOPMENT_USER_ID)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (users) => {
          this.users = users;
        },
        error: () => {
          this.errorMessage =
            'Pretraga trenutno nije dostupna. Proverite da li je server pokrenut i pokušajte ponovo.';
        },
      });
  }

  openConversation(user: User): void {
    if (this.openingUserId !== null) {
      return;
    }

    this.selectedUser = user;
    this.openingUserId = user.id;
    this.conversationErrorMessage = '';

    this.conversationService
      .openDirect(DEVELOPMENT_USER_ID, user.id)
      .pipe(finalize(() => (this.openingUserId = null)))
      .subscribe({
        next: (conversation) => {
          void this.router.navigate(['/conversations', conversation.id]);
        },
        error: () => {
          this.conversationErrorMessage =
            'Razgovor trenutno ne može da se otvori. Pokušajte ponovo.';
        },
      });
  }
}
