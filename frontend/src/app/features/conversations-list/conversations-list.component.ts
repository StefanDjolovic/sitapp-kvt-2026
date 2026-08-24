import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, EMPTY, exhaustMap, finalize, merge, of, Subject, timer } from 'rxjs';

import { DEVELOPMENT_USER_ID } from '../../core/development-user';
import { ConversationSummary } from '../../models/conversation-summary.model';
import { ConversationService } from '../../services/conversation.service';

const CONVERSATION_POLL_INTERVAL_MS = 5_000;

@Component({
  selector: 'app-conversations-list',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './conversations-list.component.html',
  styleUrl: './conversations-list.component.scss',
})
export class ConversationsListComponent implements OnInit {
  private readonly conversationService = inject(ConversationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly refreshRequests = new Subject<void>();

  readonly currentUserId = DEVELOPMENT_USER_ID;
  conversations: ConversationSummary[] = [];
  loading = false;
  errorMessage = '';

  ngOnInit(): void {
    this.loading = true;
    this.startPolling();
  }

  loadConversations(): void {
    this.loading = this.conversations.length === 0;
    this.errorMessage = '';
    this.refreshRequests.next();
  }

  private startPolling(): void {
    merge(
      of(undefined),
      timer(CONVERSATION_POLL_INTERVAL_MS, CONVERSATION_POLL_INTERVAL_MS),
      this.refreshRequests,
    )
      .pipe(
        exhaustMap(() =>
          this.conversationService.getAll(DEVELOPMENT_USER_ID).pipe(
            catchError(() => {
              if (this.conversations.length === 0) {
                this.errorMessage =
                  'Razgovori trenutno nisu dostupni. Proverite vezu sa serverom i pokušajte ponovo.';
              }
              return EMPTY;
            }),
            finalize(() => (this.loading = false)),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (conversations) => {
          this.conversations = conversations;
          this.errorMessage = '';
        },
      });
  }
}
