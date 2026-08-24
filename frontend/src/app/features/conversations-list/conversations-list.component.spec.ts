import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { ConversationSummary } from '../../models/conversation-summary.model';
import { ConversationType } from '../../models/conversation-type.model';
import { User } from '../../models/user.model';
import { ConversationService } from '../../services/conversation.service';
import { ConversationsListComponent } from './conversations-list.component';

describe('ConversationsListComponent', () => {
  let fixture: ComponentFixture<ConversationsListComponent>;
  let conversationService: jasmine.SpyObj<ConversationService>;

  const currentUser: User = {
    id: 1,
    username: 'stefan.pavlovic',
    firstName: 'Stefan',
    lastName: 'Pavlović',
    phoneNumber: '+381608888888',
  };

  function summary(id: number, title: string, sentAt: string, unreadCount = 0): ConversationSummary {
    return {
      id,
      type: ConversationType.Direct,
      title,
      otherUser: null,
      lastMessage: {
        id: id * 10,
        conversationId: id,
        sender: currentUser,
        content: `Poruka za ${title}`,
        sentAt,
      },
      unreadCount,
    };
  }

  beforeEach(async () => {
    conversationService = jasmine.createSpyObj<ConversationService>('ConversationService', [
      'getAll',
    ]);

    await TestBed.configureTestingModule({
      imports: [ConversationsListComponent],
      providers: [
        provideRouter([]),
        { provide: ConversationService, useValue: conversationService },
      ],
    }).compileComponents();
  });

  afterEach(() => fixture?.destroy());

  function createComponent(): ConversationsListComponent {
    fixture = TestBed.createComponent(ConversationsListComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  it('displays conversations in the order returned by the server', () => {
    conversationService.getAll.and.returnValue(
      of([
        summary(1, 'Stariji razgovor', '2026-08-24T08:00:00Z'),
        summary(2, 'Noviji razgovor', '2026-08-24T10:00:00Z', 3),
      ]),
    );

    const component = createComponent();

    expect(conversationService.getAll).toHaveBeenCalledOnceWith(1);
    expect(component.conversations.map((conversation) => conversation.id)).toEqual([1, 2]);
    expect(fixture.nativeElement.querySelector('.unread-badge').textContent).toContain('3');
  });

  it('shows an empty state when there are no conversations', () => {
    conversationService.getAll.and.returnValue(of([]));

    createComponent();

    expect(fixture.nativeElement.querySelector('[data-testid="conversations-empty"]')).not.toBeNull();
  });

  it('shows an error state when loading fails', () => {
    conversationService.getAll.and.returnValue(throwError(() => new Error('Network error')));

    createComponent();

    expect(fixture.nativeElement.querySelector('[data-testid="conversations-error"]')).not.toBeNull();
  });

  it('polls every five seconds and stops after the component is destroyed', fakeAsync(() => {
    conversationService.getAll.and.returnValue(of([]));
    createComponent();

    expect(conversationService.getAll).toHaveBeenCalledTimes(1);

    tick(5_000);
    expect(conversationService.getAll).toHaveBeenCalledTimes(2);

    fixture.destroy();
    tick(5_000);
    expect(conversationService.getAll).toHaveBeenCalledTimes(2);
  }));

  it('does not start another list request while the previous poll is pending', fakeAsync(() => {
    const firstResponse = new Subject<ConversationSummary[]>();
    conversationService.getAll.and.callFake(() =>
      conversationService.getAll.calls.count() === 1 ? firstResponse : of([]),
    );
    createComponent();

    tick(10_000);
    expect(conversationService.getAll).toHaveBeenCalledTimes(1);

    firstResponse.next([]);
    firstResponse.complete();
    tick(5_000);

    expect(conversationService.getAll).toHaveBeenCalledTimes(2);
  }));
});
