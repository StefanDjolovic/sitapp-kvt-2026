import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CreateDirectConversationRequest,
  DirectConversation,
} from '../models/direct-conversation.model';

@Injectable({ providedIn: 'root' })
export class ConversationService {
  private readonly http = inject(HttpClient);
  private readonly conversationsUrl = '/api/conversations';

  openDirect(currentUserId: number, otherUserId: number): Observable<DirectConversation> {
    const request: CreateDirectConversationRequest = { currentUserId, otherUserId };
    return this.http.post<DirectConversation>(`${this.conversationsUrl}/direct`, request);
  }

  getById(conversationId: number, currentUserId: number): Observable<DirectConversation> {
    const params = new HttpParams().set('currentUserId', currentUserId);
    return this.http.get<DirectConversation>(`${this.conversationsUrl}/${conversationId}`, {
      params,
    });
  }
}
