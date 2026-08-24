import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { User } from '../../models/user.model';
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

  query = '';
  users: User[] = [];
  selectedUser: User | null = null;
  loading = false;
  hasSearched = false;
  errorMessage = '';

  onQueryChange(): void {
    if (this.query.trim()) {
      return;
    }

    this.users = [];
    this.selectedUser = null;
    this.hasSearched = false;
    this.errorMessage = '';
  }

  search(): void {
    const query = this.query.trim();

    if (!query || this.loading) {
      return;
    }

    this.loading = true;
    this.hasSearched = true;
    this.errorMessage = '';
    this.users = [];
    this.selectedUser = null;

    this.userService
      .search(query)
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

  selectUser(user: User): void {
    this.selectedUser = user;
  }
}
