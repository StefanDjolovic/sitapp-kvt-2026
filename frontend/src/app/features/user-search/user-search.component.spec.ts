import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { DirectConversation } from '../../models/direct-conversation.model';
import { User } from '../../models/user.model';
import { ConversationService } from '../../services/conversation.service';
import { UserService } from '../../services/user.service';
import { UserSearchComponent } from './user-search.component';

describe('UserSearchComponent', () => {
  let fixture: ComponentFixture<UserSearchComponent>;
  let component: UserSearchComponent;
  let userService: jasmine.SpyObj<UserService>;
  let conversationService: jasmine.SpyObj<ConversationService>;
  let router: jasmine.SpyObj<Router>;

  const ana: User = {
    id: 2,
    username: 'ana.petrovic',
    firstName: 'Ana',
    lastName: 'Petrović',
    phoneNumber: '+381601111111',
  };

  beforeEach(async () => {
    userService = jasmine.createSpyObj<UserService>('UserService', ['search']);
    conversationService = jasmine.createSpyObj<ConversationService>('ConversationService', [
      'openDirect',
    ]);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.resolveTo(true);

    await TestBed.configureTestingModule({
      imports: [UserSearchComponent],
      providers: [
        { provide: UserService, useValue: userService },
        { provide: ConversationService, useValue: conversationService },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('shows the initial search hint', () => {
    expect(fixture.nativeElement.querySelector('[data-testid="initial-state"]')).not.toBeNull();
  });

  it('does not start a search when the query is blank', () => {
    component.query = '   ';

    component.search();

    expect(userService.search).not.toHaveBeenCalled();
  });

  it('clears old results when the query is removed', () => {
    component.users = [ana];
    component.selectedUser = ana;
    component.hasSearched = true;
    component.query = '';

    component.onQueryChange();

    expect(component.users).toEqual([]);
    expect(component.selectedUser).toBeNull();
    expect(component.hasSearched).toBeFalse();
  });

  it('renders users returned by the search service', () => {
    userService.search.and.returnValue(of([ana]));
    component.query = '  ana  ';

    component.search();
    fixture.detectChanges();

    expect(userService.search).toHaveBeenCalledOnceWith('ana', 1);
    expect(fixture.nativeElement.querySelector('[data-testid="user-results"]').textContent).toContain(
      'Ana Petrović',
    );
  });

  it('shows an empty state when no users match', () => {
    userService.search.and.returnValue(of([]));
    component.query = 'nepostojeci';

    component.search();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="empty-state"]')).not.toBeNull();
  });

  it('shows an error state when the request fails', () => {
    userService.search.and.returnValue(throwError(() => new Error('Network error')));
    component.query = 'ana';

    component.search();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-testid="error-state"]')).not.toBeNull();
  });

  it('opens a direct conversation and navigates to it', () => {
    userService.search.and.returnValue(of([ana]));
    conversationService.openDirect.and.returnValue(
      of({ id: 15, otherUser: ana, createdAt: '2026-08-24T10:30:00Z' }),
    );
    component.query = 'ana';
    component.search();
    fixture.detectChanges();

    const result = fixture.nativeElement.querySelector('.user-card') as HTMLButtonElement;
    result.click();
    fixture.detectChanges();

    expect(conversationService.openDirect).toHaveBeenCalledOnceWith(1, 2);
    expect(router.navigate).toHaveBeenCalledOnceWith(['/conversations', 15]);
    expect(userService.search).toHaveBeenCalledTimes(1);
  });

  it('shows opening and error states for a failed conversation request', () => {
    const pendingConversation = new Subject<DirectConversation>();
    conversationService.openDirect.and.returnValue(pendingConversation);

    component.openConversation(ana);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="opening-conversation"]')).not.toBeNull();

    pendingConversation.error(new Error('Network error'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-testid="conversation-error"]')).not.toBeNull();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
