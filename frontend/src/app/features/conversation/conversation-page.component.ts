import { DatePipe } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { DEVELOPMENT_USER_ID } from '../../core/development-user';
import { DirectConversation } from '../../models/direct-conversation.model';
import { ConversationService } from '../../services/conversation.service';

@Component({
  selector: 'app-conversation-page',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './conversation-page.component.html',
  styleUrl: './conversation-page.component.scss',
})
export class ConversationPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly conversationService = inject(ConversationService);

  conversation: DirectConversation | null = null;
  loading = false;
  errorMessage = '';
  private conversationId: number | null = null;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!Number.isSafeInteger(id) || id <= 0) {
      this.loading = false;
      this.errorMessage = 'Adresa razgovora nije ispravna.';
      return;
    }

    this.conversationId = id;
    this.loadConversation();
  }

  retry(): void {
    this.loadConversation();
  }

  private loadConversation(): void {
    if (this.conversationId === null || this.loading) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.conversation = null;

    this.conversationService
      .getById(this.conversationId, DEVELOPMENT_USER_ID)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (conversation) => {
          this.conversation = conversation;
        },
        error: () => {
          this.errorMessage =
            'Razgovor trenutno nije dostupan. Vratite se na pretragu ili pokušajte ponovo.';
        },
      });
  }
}
