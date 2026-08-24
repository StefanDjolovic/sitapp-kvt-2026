import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DirectConversation } from '../models/direct-conversation.model';
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
});
