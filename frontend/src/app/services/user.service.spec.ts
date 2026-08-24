import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { User } from '../models/user.model';
import { UserService } from './user.service';

describe('UserService', () => {
  let service: UserService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(UserService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('searches users with a trimmed query', () => {
    const users: User[] = [
      {
        id: 2,
        username: 'ana.petrovic',
        firstName: 'Ana',
        lastName: 'Petrović',
        phoneNumber: '+381601111111',
      },
    ];

    service.search('  ana  ').subscribe((response) => expect(response).toEqual(users));

    const request = httpTesting.expectOne(
      (candidate) =>
        candidate.url === '/api/users/search' && candidate.params.get('query') === 'ana',
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.has('currentUserId')).toBeFalse();
    request.flush(users);
  });

  it('sends the current user id when it is provided', () => {
    service.search('marko', 1).subscribe();

    const request = httpTesting.expectOne(
      (candidate) =>
        candidate.url === '/api/users/search' &&
        candidate.params.get('query') === 'marko' &&
        candidate.params.get('currentUserId') === '1',
    );
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });
});
