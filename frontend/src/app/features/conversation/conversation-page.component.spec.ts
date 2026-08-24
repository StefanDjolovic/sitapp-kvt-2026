import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { DirectConversation } from '../../models/direct-conversation.model';
import { ConversationService } from '../../services/conversation.service';
import { ConversationPageComponent } from './conversation-page.component';

describe('ConversationPageComponent', () => {
  let conversationService: jasmine.SpyObj<ConversationService>;

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

  beforeEach(async () => {
    conversationService = jasmine.createSpyObj<ConversationService>('ConversationService', [
      'getById',
    ]);

    await TestBed.configureTestingModule({
      imports: [ConversationPageComponent],
      providers: [
        provideRouter([]),
        { provide: ConversationService, useValue: conversationService },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '15' }) } },
        },
      ],
    }).compileComponents();
  });

  function createComponent(): ComponentFixture<ConversationPageComponent> {
    const fixture = TestBed.createComponent(ConversationPageComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('loads and displays the selected conversation', () => {
    conversationService.getById.and.returnValue(of(conversation));

    const fixture = createComponent();

    expect(conversationService.getById).toHaveBeenCalledOnceWith(15, 1);
    expect(fixture.nativeElement.querySelector('h1').textContent).toContain('Ana Petrović');
    expect(fixture.nativeElement.querySelector('[data-testid="messages-empty"]')).not.toBeNull();
  });

  it('shows an error state when loading fails', () => {
    conversationService.getById.and.returnValue(throwError(() => new Error('Not found')));

    const fixture = createComponent();

    expect(fixture.nativeElement.querySelector('[data-testid="conversation-error"]')).not.toBeNull();
  });
});
