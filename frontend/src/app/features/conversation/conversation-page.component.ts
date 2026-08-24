import { DatePipe } from '@angular/common';
import {
  Component,
  DestroyRef,
  ElementRef,
  inject,
  OnInit,
  ViewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, EMPTY, exhaustMap, finalize, merge, of, Subject, timer } from 'rxjs';

import { DEVELOPMENT_USER_ID } from '../../core/development-user';
import { ConversationDetails } from '../../models/conversation-details.model';
import { ConversationType } from '../../models/conversation-type.model';
import { Message } from '../../models/message.model';
import { ConversationService } from '../../services/conversation.service';

const MESSAGE_POLL_INTERVAL_MS = 5_000;

@Component({
  selector: 'app-conversation-page',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './conversation-page.component.html',
  styleUrl: './conversation-page.component.scss',
})
export class ConversationPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly conversationService = inject(ConversationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly refreshMessagesRequests = new Subject<void>();

  @ViewChild('messageViewport') private messageViewport?: ElementRef<HTMLElement>;

  readonly currentUserId = DEVELOPMENT_USER_ID;
  readonly conversationType = ConversationType;
  conversation: ConversationDetails | null = null;
  messages: Message[] = [];
  draft = '';
  conversationLoading = false;
  messagesLoading = false;
  sending = false;
  conversationErrorMessage = '';
  messagesErrorMessage = '';
  sendErrorMessage = '';

  private conversationId: number | null = null;
  private lastMarkedMessageId: number | null = null;
  private messagePollingStarted = false;
  private initialMessagesDisplayed = false;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!Number.isSafeInteger(id) || id <= 0) {
      this.conversationErrorMessage = 'Adresa razgovora nije ispravna.';
      return;
    }

    this.conversationId = id;
    this.loadConversation();
  }

  retryConversation(): void {
    this.loadConversation();
  }

  retryMessages(): void {
    if (
      this.conversationId === null ||
      !this.messagePollingStarted ||
      this.messagesLoading
    ) {
      return;
    }

    this.messagesLoading = this.messages.length === 0;
    this.messagesErrorMessage = '';
    this.refreshMessagesRequests.next();
  }

  onMessageScroll(): void {
    if (this.isNearBottom()) {
      this.markDisplayedMessagesAsRead();
    }
  }

  sendMessage(): void {
    const content = this.draft.trim();

    if (!content || this.sending || this.conversationId === null) {
      return;
    }

    this.sending = true;
    this.sendErrorMessage = '';

    this.conversationService
      .sendMessage(this.conversationId, DEVELOPMENT_USER_ID, content)
      .pipe(
        finalize(() => (this.sending = false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (message) => {
          this.draft = '';
          this.upsertMessage(message);
          this.scrollToLatest();
        },
        error: () => {
          this.sendErrorMessage = 'Poruka nije poslata. Proverite vezu i pokušajte ponovo.';
        },
      });
  }

  private loadConversation(): void {
    if (this.conversationId === null || this.conversationLoading) {
      return;
    }

    this.conversationLoading = true;
    this.conversationErrorMessage = '';

    this.conversationService
      .getById(this.conversationId, DEVELOPMENT_USER_ID)
      .pipe(
        finalize(() => (this.conversationLoading = false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (conversation) => {
          this.conversation = conversation;
          this.startMessagePollingOnce();
        },
        error: () => {
          this.conversation = null;
          this.conversationErrorMessage =
            'Razgovor trenutno nije dostupan. Vratite se na listu ili pokušajte ponovo.';
        },
      });
  }

  private startMessagePollingOnce(): void {
    if (this.conversationId === null || this.messagePollingStarted) {
      return;
    }

    this.messagePollingStarted = true;
    this.messagesLoading = true;
    this.startMessagePolling(this.conversationId);
  }

  private startMessagePolling(conversationId: number): void {
    merge(
      of(undefined),
      timer(MESSAGE_POLL_INTERVAL_MS, MESSAGE_POLL_INTERVAL_MS),
      this.refreshMessagesRequests,
    )
      .pipe(
        exhaustMap(() =>
          this.conversationService.getMessages(conversationId, DEVELOPMENT_USER_ID).pipe(
            catchError(() => {
              this.messagesLoading = false;
              this.messagesErrorMessage =
                'Poruke trenutno nisu dostupne. Pokušajte ponovo za nekoliko trenutaka.';
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((messages) => this.displayMessages(messages));
  }

  private displayMessages(messages: Message[]): void {
    const isInitialDisplay = !this.initialMessagesDisplayed;
    const wasNearBottom = this.isNearBottom();
    const displayedMessageIds = new Set(this.messages.map((message) => message.id));
    const hasNewMessages = messages.some((message) => !displayedMessageIds.has(message.id));

    this.messages = this.mergeMessages(this.messages, messages);
    this.initialMessagesDisplayed = true;
    this.messagesLoading = false;
    this.messagesErrorMessage = '';
    if (isInitialDisplay || wasNearBottom) {
      this.markDisplayedMessagesAsRead();
    }

    if (isInitialDisplay || (hasNewMessages && wasNearBottom)) {
      this.scrollToLatest();
    }
  }

  private markDisplayedMessagesAsRead(): void {
    const lastMessage = this.messages.at(-1);

    if (
      this.conversationId === null ||
      !lastMessage ||
      lastMessage.id === this.lastMarkedMessageId
    ) {
      return;
    }

    this.conversationService
      .markAsRead(this.conversationId, DEVELOPMENT_USER_ID, lastMessage.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.lastMarkedMessageId = lastMessage.id;
        },
        error: () => {
          // Poruke ostaju prikazane; marker će se ponovo poslati pri sledećem poll-u.
        },
      });
  }

  private upsertMessage(message: Message): void {
    this.messages = this.mergeMessages(this.messages, [message]);
    this.messagesErrorMessage = '';
  }

  private mergeMessages(currentMessages: Message[], receivedMessages: Message[]): Message[] {
    const messagesById = new Map<number, Message>();

    for (const message of currentMessages) {
      messagesById.set(message.id, message);
    }

    for (const message of receivedMessages) {
      messagesById.set(message.id, message);
    }

    return this.sortMessages([...messagesById.values()]);
  }

  private sortMessages(messages: Message[]): Message[] {
    return [...messages].sort((first, second) => {
      const timeDifference = Date.parse(first.sentAt) - Date.parse(second.sentAt);
      return timeDifference || first.id - second.id;
    });
  }

  private scrollToLatest(): void {
    queueMicrotask(() => {
      const viewport = this.messageViewport?.nativeElement;
      if (viewport) {
        viewport.scrollTop = viewport.scrollHeight;
      }
    });
  }

  private isNearBottom(): boolean {
    const viewport = this.messageViewport?.nativeElement;

    if (!viewport) {
      return true;
    }

    return viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight <= 80;
  }
}
