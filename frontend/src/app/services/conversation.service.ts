import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CreateDirectConversationRequest,
  DirectConversation,
} from '../models/direct-conversation.model';
import { ConversationDetails } from '../models/conversation-details.model';
import { ConversationSummary } from '../models/conversation-summary.model';
import {
  MarkConversationReadRequest,
  Message,
  SendMessageRequest,
} from '../models/message.model';

@Injectable({ providedIn: 'root' })
export class ConversationService {
  private readonly http = inject(HttpClient);
  private readonly conversationsUrl = '/api/conversations';

  openDirect(currentUserId: number, otherUserId: number): Observable<DirectConversation> {
    const request: CreateDirectConversationRequest = { currentUserId, otherUserId };
    return this.http.post<DirectConversation>(`${this.conversationsUrl}/direct`, request);
  }

  getById(conversationId: number, currentUserId: number): Observable<ConversationDetails> {
    const params = new HttpParams().set('currentUserId', currentUserId);
    return this.http.get<ConversationDetails>(`${this.conversationsUrl}/${conversationId}`, {
      params,
    });
  }

  getAll(currentUserId: number): Observable<ConversationSummary[]> {
    const params = new HttpParams().set('currentUserId', currentUserId);
    return this.http.get<ConversationSummary[]>(this.conversationsUrl, { params });
  }

  getMessages(conversationId: number, currentUserId: number): Observable<Message[]> {
    const params = new HttpParams().set('currentUserId', currentUserId);
    return this.http.get<Message[]>(`${this.conversationsUrl}/${conversationId}/messages`, {
      params,
    });
  }

  sendMessage(conversationId: number, senderId: number, content: string): Observable<Message> {
    const request: SendMessageRequest = { senderId, content };
    return this.http.post<Message>(`${this.conversationsUrl}/${conversationId}/messages`, request);
  }

  markAsRead(
    conversationId: number,
    currentUserId: number,
    lastSeenMessageId: number,
  ): Observable<void> {
    const params = new HttpParams().set('currentUserId', currentUserId);
    const request: MarkConversationReadRequest = { lastSeenMessageId };
    return this.http.put<void>(`${this.conversationsUrl}/${conversationId}/read`, request, {
      params,
    });
  }
}
