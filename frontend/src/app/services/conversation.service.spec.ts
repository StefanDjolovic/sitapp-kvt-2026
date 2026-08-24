import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ConversationSummary } from '../models/conversation-summary.model';
import { ConversationType } from '../models/conversation-type.model';
import { DirectConversation } from '../models/direct-conversation.model';
import { Message } from '../models/message.model';
import { ConversationService } from './conversation.service';

describe('ConversationService', () => {
  let service: ConversationService;
  let httpTesting: HttpTestingController;

  const conversation: DirectConversation = {
    id: 15,
    otherUser: {
      id: 2,
      username: 'ana.petrovic',
      firstName: 'Ana',
      lastName: 'Petrović',
      phoneNumber: '+381601111111',
    },
    createdAt: '2026-08-24T10:30:00Z',
  };
  const message: Message = {
    id: 20,
    conversationId: 15,
    sender: conversation.otherUser,
    content: 'Zdravo!',
    sentAt: '2026-08-24T10:31:00Z',
  };
  const summary: ConversationSummary = {
    id: 15,
    type: ConversationType.Direct,
    title: 'Ana Petrović',
    otherUser: conversation.otherUser,
    lastMessage: message,
    unreadCount: 1,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ConversationService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('opens a direct conversation for two users', () => {
    service.openDirect(1, 2).subscribe((response) => expect(response).toEqual(conversation));

    const request = httpTesting.expectOne('/api/conversations/direct');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ currentUserId: 1, otherUserId: 2 });
    request.flush(conversation);
  });

  it('loads a conversation for the current user', () => {
    service.getById(15, 1).subscribe((response) => expect(response).toEqual(conversation));

    const request = httpTesting.expectOne(
      (candidate) =>
        candidate.url === '/api/conversations/15' &&
        candidate.params.get('currentUserId') === '1',
    );
    expect(request.request.method).toBe('GET');
    request.flush(conversation);
  });

  it('loads the current user conversations', () => {
    service.getAll(1).subscribe((response) => expect(response).toEqual([summary]));

    const request = httpTesting.expectOne(
      (candidate) =>
        candidate.url === '/api/conversations' &&
        candidate.params.get('currentUserId') === '1',
    );
    expect(request.request.method).toBe('GET');
    request.flush([summary]);
  });

  it('loads messages from a conversation', () => {
    service.getMessages(15, 1).subscribe((response) => expect(response).toEqual([message]));

    const request = httpTesting.expectOne(
      (candidate) =>
        candidate.url === '/api/conversations/15/messages' &&
        candidate.params.get('currentUserId') === '1',
    );
    expect(request.request.method).toBe('GET');
    request.flush([message]);
  });

  it('sends a text message', () => {
    service.sendMessage(15, 1, 'Zdravo!').subscribe((response) => expect(response).toEqual(message));

    const request = httpTesting.expectOne('/api/conversations/15/messages');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ senderId: 1, content: 'Zdravo!' });
    request.flush(message);
  });

  it('marks the last displayed message as read', () => {
    service.markAsRead(15, 1, 20).subscribe();

    const request = httpTesting.expectOne(
      (candidate) =>
        candidate.url === '/api/conversations/15/read' &&
        candidate.params.get('currentUserId') === '1',
    );
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ lastSeenMessageId: 20 });
    request.flush(null);
  });
});
