import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { DirectConversation } from '../../models/direct-conversation.model';
import { Message } from '../../models/message.model';
import { User } from '../../models/user.model';
import { ConversationService } from '../../services/conversation.service';
import { ConversationPageComponent } from './conversation-page.component';

describe('ConversationPageComponent', () => {
  let fixture: ComponentFixture<ConversationPageComponent> | undefined;
  let conversationService: jasmine.SpyObj<ConversationService>;

  const currentUser: User = {
    id: 1,
    username: 'stefan.pavlovic',
    firstName: 'Stefan',
    lastName: 'Pavlović',
    phoneNumber: '+381608888888',
  };
  const ana: User = {
    id: 2,
    username: 'ana.petrovic',
    firstName: 'Ana',
    lastName: 'Petrović',
    phoneNumber: '+381601111111',
  };
  const conversation: DirectConversation = {
    id: 15,
    otherUser: ana,
    createdAt: '2026-08-24T10:30:00Z',
  };
  const firstMessage: Message = {
    id: 20,
    conversationId: 15,
    sender: ana,
    content: 'Zdravo!',
    sentAt: '2026-08-24T10:31:00Z',
  };
  const secondMessage: Message = {
    id: 21,
    conversationId: 15,
    sender: currentUser,
    content: 'Ćao!',
    sentAt: '2026-08-24T10:32:00Z',
  };
  const thirdMessage: Message = {
    id: 22,
    conversationId: 15,
    sender: ana,
    content: 'Nova poruka',
    sentAt: '2026-08-24T10:33:00Z',
  };
  const fourthMessage: Message = {
    id: 23,
    conversationId: 15,
    sender: ana,
    content: 'Još jedna poruka',
    sentAt: '2026-08-24T10:34:00Z',
  };

  beforeEach(async () => {
    conversationService = jasmine.createSpyObj<ConversationService>('ConversationService', [
      'getById',
      'getMessages',
      'sendMessage',
      'markAsRead',
    ]);
    conversationService.getById.and.returnValue(of(conversation));
    conversationService.getMessages.and.returnValue(of([secondMessage, firstMessage]));
    conversationService.markAsRead.and.returnValue(of(undefined));

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

  afterEach(() => fixture?.destroy());

  function createComponent(): ConversationPageComponent {
    fixture = TestBed.createComponent(ConversationPageComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('loads messages chronologically and marks the last displayed message as read', () => {
    const component = createComponent();

    expect(conversationService.getById).toHaveBeenCalledOnceWith(15, 1);
    expect(conversationService.getMessages).toHaveBeenCalledOnceWith(15, 1);
    expect(component.messages.map((message) => message.id)).toEqual([20, 21]);
    expect(conversationService.markAsRead).toHaveBeenCalledOnceWith(15, 1, 21);
    expect(fixture?.nativeElement.querySelector('[data-testid="message-list"]')).not.toBeNull();
  });

  it('starts loading messages only after conversation details are loaded', () => {
    const conversationDetails = new Subject<DirectConversation>();
    conversationService.getById.and.returnValue(conversationDetails);

    createComponent();

    expect(conversationService.getMessages).not.toHaveBeenCalled();
    expect(conversationService.markAsRead).not.toHaveBeenCalled();

    conversationDetails.next(conversation);

    expect(conversationService.getMessages).toHaveBeenCalledOnceWith(15, 1);
    expect(conversationService.markAsRead).toHaveBeenCalledOnceWith(15, 1, 21);
  });

  it('does not start message polling if conversation details arrive after destroy', () => {
    const conversationDetails = new Subject<DirectConversation>();
    conversationService.getById.and.returnValue(conversationDetails);

    createComponent();
    fixture?.destroy();
    fixture = undefined;
    conversationDetails.next(conversation);

    expect(conversationService.getMessages).not.toHaveBeenCalled();
    expect(conversationService.markAsRead).not.toHaveBeenCalled();
  });

  it('starts one polling stream when conversation retry succeeds', fakeAsync(() => {
    conversationService.getById.and.returnValue(
      throwError(() => new Error('Network error')),
    );
    conversationService.getMessages.and.returnValue(of([]));
    const component = createComponent();

    expect(conversationService.getMessages).not.toHaveBeenCalled();

    conversationService.getById.and.returnValue(of(conversation));
    component.retryConversation();
    component.retryConversation();

    expect(conversationService.getMessages).toHaveBeenCalledTimes(1);

    tick(5_000);
    expect(conversationService.getMessages).toHaveBeenCalledTimes(2);
  }));

  it('does not mark an empty conversation as read', () => {
    conversationService.getMessages.and.returnValue(of([]));

    createComponent();

    expect(conversationService.markAsRead).not.toHaveBeenCalled();
    expect(fixture?.nativeElement.querySelector('[data-testid="messages-empty"]')).not.toBeNull();
  });

  it('sends a trimmed message and appends the response', () => {
    conversationService.getMessages.and.returnValue(of([]));
    conversationService.sendMessage.and.returnValue(of(secondMessage));
    const component = createComponent();
    const scrollSpy = spyOn<any>(component, 'scrollToLatest');
    component.draft = '  Ćao!  ';

    component.sendMessage();
    fixture?.detectChanges();

    expect(conversationService.sendMessage).toHaveBeenCalledOnceWith(15, 1, 'Ćao!');
    expect(component.messages).toEqual([secondMessage]);
    expect(component.draft).toBe('');
    expect(scrollSpy).toHaveBeenCalledOnceWith();
  });

  it('keeps a sent message when an older polling response arrives afterwards', () => {
    const messagesResponse = new Subject<Message[]>();
    conversationService.getMessages.and.returnValue(messagesResponse);
    conversationService.sendMessage.and.returnValue(of(secondMessage));
    const component = createComponent();
    component.draft = 'Ćao!';

    component.sendMessage();
    messagesResponse.next([firstMessage]);

    expect(component.messages.map((message) => message.id)).toEqual([20, 21]);
  });

  it('shows an error when messages cannot be loaded', () => {
    conversationService.getMessages.and.returnValue(
      throwError(() => new Error('Network error')),
    );

    createComponent();

    expect(fixture?.nativeElement.querySelector('[data-testid="messages-error"]')).not.toBeNull();
  });

  it('polls every five seconds and stops after the component is destroyed', fakeAsync(() => {
    conversationService.getMessages.and.returnValue(of([]));
    createComponent();
    expect(conversationService.getMessages).toHaveBeenCalledTimes(1);

    tick(5_000);
    expect(conversationService.getMessages).toHaveBeenCalledTimes(2);

    fixture?.destroy();
    fixture = undefined;
    tick(5_000);
    expect(conversationService.getMessages).toHaveBeenCalledTimes(2);
  }));

  it('scrolls initially and only follows new polled messages when already near the bottom', fakeAsync(() => {
    let response = [firstMessage];
    conversationService.getMessages.and.callFake(() => of(response));

    fixture = TestBed.createComponent(ConversationPageComponent);
    const component = fixture.componentInstance;
    const scrollSpy = spyOn<any>(component, 'scrollToLatest');
    const nearBottomSpy = spyOn<any>(component, 'isNearBottom').and.returnValue(false);
    fixture.detectChanges();

    expect(scrollSpy).toHaveBeenCalledTimes(1);

    tick(5_000);
    expect(scrollSpy).toHaveBeenCalledTimes(1);

    response = [firstMessage, thirdMessage];
    tick(5_000);
    expect(scrollSpy).toHaveBeenCalledTimes(1);

    nearBottomSpy.and.returnValue(true);
    response = [firstMessage, thirdMessage, fourthMessage];
    tick(5_000);
    expect(scrollSpy).toHaveBeenCalledTimes(2);
  }));
});
