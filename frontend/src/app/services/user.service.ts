import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly searchUrl = '/api/users/search';

  search(query: string, currentUserId?: number): Observable<User[]> {
    let params = new HttpParams().set('query', query.trim());

    if (currentUserId !== undefined) {
      params = params.set('currentUserId', currentUserId);
    }

    return this.http.get<User[]>(this.searchUrl, { params });
  }
}
