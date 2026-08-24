import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { User } from '../../models/user.model';
import { UserService } from '../../services/user.service';
import { UserSearchComponent } from './user-search.component';

describe('UserSearchComponent', () => {
  let fixture: ComponentFixture<UserSearchComponent>;
  let component: UserSearchComponent;
  let userService: jasmine.SpyObj<UserService>;

  const ana: User = {
    id: 2,
    username: 'ana.petrovic',
    firstName: 'Ana',
    lastName: 'Petrović',
    phoneNumber: '+381601111111',
  };

  beforeEach(async () => {
    userService = jasmine.createSpyObj<UserService>('UserService', ['search']);

    await TestBed.configureTestingModule({
      imports: [UserSearchComponent],
      providers: [{ provide: UserService, useValue: userService }],
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

    expect(userService.search).toHaveBeenCalledOnceWith('ana');
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

  it('selects a user without making another API request', () => {
    userService.search.and.returnValue(of([ana]));
    component.query = 'ana';
    component.search();
    fixture.detectChanges();

    const result = fixture.nativeElement.querySelector('.user-card') as HTMLButtonElement;
    result.click();
    fixture.detectChanges();

    expect(component.selectedUser).toEqual(ana);
    expect(userService.search).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.querySelector('[data-testid="selected-user"]').textContent).toContain(
      'Razgovor još nije kreiran',
    );
  });
});
