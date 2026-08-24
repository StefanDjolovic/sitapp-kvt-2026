import { User } from './user.model';

export interface Message {
  id: number;
  conversationId: number;
  sender: User;
  content: string;
  sentAt: string;
}

export interface SendMessageRequest {
  senderId: number;
  content: string;
}

export interface MarkConversationReadRequest {
  lastSeenMessageId: number;
}
